package com.lxbluem.rest;

import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.SerializedRequest;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lxbluem.common.infrastructure.Address.ROUTE_ADD;

public class RouterVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(RouterVerticle.class);

    private final Map<String, AtomicInteger> verticleCounter = new HashMap<>();

    @Override
    public void start(Promise<Void> start) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route("/eventbus/*").subRouter(eventBusHandler());
        router.route("/sev/*").subRouter(sockjsHandler());
        router.route().last().handler(StaticHandler.create());

        Router api = Router.router(vertx);
        api.route("/time").handler(rc -> rc.response().end(String.valueOf(Instant.now().toEpochMilli())));
        router.route("/api/*").subRouter(api);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, event -> {
                    if (event.succeeded()) {
                        LOG.info("listening on port {}  [deployid:{}]", event.result().actualPort(), deploymentID());
                        start.complete();
                    } else {
                        start.fail(event.cause());
                    }
                });

        vertx.eventBus().consumer(ROUTE_ADD.address(), message -> setupRouter(api, message));
    }

    private Router sockjsHandler() {
        SockJSHandler handler = SockJSHandler.create(vertx);

        return handler.socketHandler(socket -> {
            LOG.info("new websocket from {}", socket.remoteAddress());
            Handler<Message<Object>> messageHandler = message -> {
                JsonObject body = (JsonObject) message.body();
                JsonObject returnMessage = new JsonObject()
                        .put("type", message.address())
                        .mergeIn(body);
                socket.write(returnMessage.encode());
            };

            Arrays.asList(
                    Address.STATE,
                    Address.REMOVED_STALE_BOTS,
                    Address.BOT_INITIALIZED,
                    Address.BOT_NOTICE,
                    Address.BOT_NICK_UPDATED,
                    Address.BOT_EXITED,
                    Address.BOT_QUEUED,
                    Address.BOT_FAILED,
                    Address.DCC_INITIALIZE,
                    Address.DCC_STARTED,
                    Address.DCC_PROGRESSED,
                    Address.DCC_FINISHED,
                    Address.DCC_FAILED
            ).forEach(address -> vertx.eventBus().consumer(address.address(), messageHandler));
        });
    }

    private Router eventBusHandler() {
        PermittedOptions permitted = new PermittedOptions().setAddressRegex(".*");

        SockJSBridgeOptions options = new SockJSBridgeOptions()
                .addInboundPermitted(permitted)
                .addOutboundPermitted(permitted);
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
                            .body(rc.body().asString())
                            .headers(headers)
                            .params(params)
                            .build();

                    vertx.eventBus()
                            .<JsonObject>rxRequest(target, JsonObject.mapFrom(serializedRequest))
                            .map(Message::body)
                            .subscribe(
                                    messageBody -> sendHttpResponse(rc.response(), messageBody),
                                    throwable -> {
                                        String reasonPhrase = throwable.getMessage().replaceAll("[\n\r]", "");
                                        String statusMessage = String.format("unsuccessful: '%s'", reasonPhrase);
                                        LOG.warn("could not serve route {} -> {} ({})", path, target, statusMessage);
                                        rc.response()
                                                .setStatusCode(400)
                                                .setStatusMessage(statusMessage)
                                                .end();
                                    }
                            );
                });

    }

    private void sendHttpResponse(HttpServerResponse response, JsonObject replyMessage) {
        StringBuilder replyMessageEncoded = new StringBuilder();
        if (replyMessage != null) {
            replyMessageEncoded.append(replyMessage.encode());
        }
        Buffer buffer = Buffer.buffer(replyMessageEncoded.toString());
        response.putHeader("content-type", "application/json")
                .putHeader("x-by", "xdcc")
                .end(buffer);
    }

}
