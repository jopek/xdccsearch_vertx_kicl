package com.lxbluem;

import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RouterVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(RouterVerticle.class);

    private Map<String, AtomicInteger> verticleCounter = new HashMap<>();

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

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

        vertx.eventBus()
                .consumer("route", message -> setupRouter(router, message));

        Route handler = router
                .route("/ppp")
                .handler(System.out::println);
    }

    private void unroute(Router router, Message<Object> message) {
        String target = ((JsonObject) message.body()).getString("target");
        verticleCounter.get(target).decrementAndGet();
        publishVerticleRouteStats();
    }

    private void publishVerticleRouteStats() {
        vertx.eventBus().publish("router.stats", new JsonObject().put("verticles", verticleCounter));
    }

    private void setupRouter(Router router, Message<Object> newRouteRequestMessage) {
        JsonObject newRouteRequestMessageBody = (JsonObject) newRouteRequestMessage.body();
        String path = newRouteRequestMessageBody.getString("path");
        String target = newRouteRequestMessageBody.getString("target");
        String method = newRouteRequestMessageBody.getString("method");

        HttpMethod httpMethod = HttpMethod.valueOf(method);

        verticleCounter.putIfAbsent(target, new AtomicInteger(0));
        verticleCounter.get(target).incrementAndGet();
        publishVerticleRouteStats();

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
                            .rxSend(target, JsonObject.mapFrom(serializedRequest))
                            .map(objectMessage -> (JsonObject) objectMessage.body())
                            .doOnEach(System.out::println)
                            .subscribe(
                                    messageBody -> sendHttpResponse(rc.response(), messageBody),
                                    throwable -> {
                                        LOG.warn("could not serve route {} -> {}", path, target, throwable.getCause());
                                        rc.response()
                                                .setStatusCode(400)
                                                .setStatusMessage("somehow not successful")
                                                .end();
                                    }
                            );
                });

    }

    private void sendHttpResponse(HttpServerResponse response, JsonObject replyMessage) {
        Buffer buffer = Buffer.buffer(replyMessage.encode());
        response.end(buffer);
    }

}
