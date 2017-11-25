package com.lxbluem;

import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lxbluem.Addresses.ROUTE_ADD;
import static java.time.Instant.now;

public class RouterVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(RouterVerticle.class);

    private Map<String, AtomicInteger> verticleCounter = new HashMap<>();

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route("/eventbus/*").handler(eventBusHandler());
        router.route().last().handler(StaticHandler.create());

        Router api = Router.router(vertx);
        api.route("/time").handler(rc -> rc.response().end(String.valueOf(Instant.now().toEpochMilli())));
        router.mountSubRouter("/api", api);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, event -> {
                    if (event.succeeded()) {
                        LOG.info("listening on port {}  [deployid:{}]", event.result().actualPort(), deploymentID());
                        startFuture.complete();
                    } else {
                        startFuture.fail(event.cause());
                    }
                });

        vertx.eventBus().consumer(ROUTE_ADD, message -> setupRouter(api, message));
    }

    private void unroute(Router router, Message<Object> message) {
        String target = ((JsonObject) message.body()).getString("target");
        verticleCounter.get(target).decrementAndGet();
    }

    private SockJSHandler eventBusHandler() {
        PermittedOptions permitted = new PermittedOptions().setAddressRegex(".*");
        BridgeOptions options = new BridgeOptions().addOutboundPermitted(permitted);
        return SockJSHandler
                .create(vertx)
                .bridge(options, event -> {
                    if (event.type() == BridgeEventType.SOCKET_CREATED) {
                        LOG.info("A socket was created");
                    }
                    event.complete(true);
                });
    }


    private void setupRouter(Router router, Message<Object> newRouteRequestMessage) {
        JsonObject newRouteRequestMessageBody = (JsonObject) newRouteRequestMessage.body();
        String path = newRouteRequestMessageBody.getString("path");
        String target = newRouteRequestMessageBody.getString("target");
        String method = newRouteRequestMessageBody.getString("method");

        HttpMethod httpMethod = HttpMethod.valueOf(method);

        verticleCounter.putIfAbsent(target, new AtomicInteger(0));
        verticleCounter.get(target).incrementAndGet();

        router.route(httpMethod, path)
                .produces("application/json")
                .handler(rc -> {

                    Map<String, String> params = new HashMap<>();
                    HttpServerRequest httpServerRequest = rc.request();

                    httpServerRequest.params()
                            .getDelegate()
                            .forEach(e -> params.put(e.getKey(), e.getValue()));

                    Map<String, String> headers = new HashMap<>();
                    httpServerRequest.headers()
                            .getDelegate()
                            .forEach(e -> headers.put(e.getKey(), e.getValue()));

                    SerializedRequest serializedRequest = SerializedRequest.builder()
                            .method(method)
                            .body(rc.getBody().toString())
                            .headers(headers)
                            .params(params)
                            .build();

                    vertx.eventBus()
                            .<JsonObject>rxSend(target, JsonObject.mapFrom(serializedRequest))
                            .map(Message::body)
                            .subscribe(
                                    messageBody -> sendHttpResponse(rc.response(), messageBody),
                                    throwable -> {
                                        LOG.warn("could not serve route {} -> {}", path, target, throwable.getCause());
                                        rc.response()
                                                .setStatusCode(400)
                                                .setStatusMessage(String.format("unsuccessful: '%s'", throwable.getMessage()))
                                                .end();
                                    }
                            );
                });

    }

    private void sendHttpResponse(HttpServerResponse response, JsonObject replyMessage) {
        StringBuilder replyMessageEncoded = new StringBuilder();
        if (replyMessage!=null) {
            replyMessageEncoded.append(replyMessage.encode());
        }
        Buffer buffer = Buffer.buffer(replyMessageEncoded.toString());
        response.putHeader("content-type", "application/json")
                .putHeader("x-by", "xdcc")
                .end(buffer);
    }

}
