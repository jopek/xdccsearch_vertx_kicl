package com.lxbluem;

import com.lxbluem.model.RouterRegistryMessage;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static com.lxbluem.Addresses.ROUTE_ADD;
import static com.lxbluem.Addresses.ROUTE_REMOVE;
import static java.lang.String.format;

public abstract class AbstractRouteVerticle extends AbstractVerticle {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    protected Future<Void> registerRouteWithHandler(
            HttpMethod httpMethod,
            String route,
            BiConsumer<SerializedRequest, Future<JsonObject>> requestHandler) {

        String target = format("%s:%s:%s", getClass().getSimpleName(), httpMethod, route);
        RouterRegistryMessage routerRegistryMessage = RouterRegistryMessage.builder()
                .method(httpMethod.name())
                .path(route)
                .target(target)
                .build();

        vertx.eventBus().publish(ROUTE_ADD, JsonObject.mapFrom(routerRegistryMessage));

        Promise<Void> promise = Promise.promise();
        vertx.eventBus()
                .consumer(target)
                .handler(ebMessage -> {
                    SerializedRequest request = Json.decodeValue(ebMessage.body().toString(), SerializedRequest.class);
                    Future future = Future.future(ebMessage::reply)
                            .onFailure(e -> ebMessage.fail(500, e.getCause().getMessage()));
                    requestHandler.accept(request, future);
                })
                .completionHandler(c -> promise.complete())
        ;
        return promise.future();
    }

    protected Future<Void> promisedRegisterRouteWithHandler(
            HttpMethod httpMethod,
            String route,
            BiConsumer<SerializedRequest, Promise<JsonObject>> requestHandler) {

        String target = format("%s:%s:%s", getClass().getSimpleName(), httpMethod, route);
        JsonObject routerRegistryMessageJsonObject = new JsonObject()
                .put("method", httpMethod.name())
                .put("route", route)
                .put("target", target);

        vertx.eventBus().publish(ROUTE_ADD, routerRegistryMessageJsonObject);

        Promise<Void> consumerSetupCompletion = Promise.promise();
        vertx.eventBus()
                .consumer(target)
                .handler(message -> {
                    SerializedRequest request = Json.decodeValue(message.body().toString(), SerializedRequest.class);
                    Promise<JsonObject> promise = Promise.promise();
                    promise.future()
                            .onSuccess(message::reply)
                            .onFailure(e -> {
                                Throwable cause = e.getCause();
                                String causeMessage = Objects.isNull(cause) ? e.getMessage() : cause.getMessage();
                                message.fail(500, causeMessage);
                            });
                    requestHandler.accept(request, promise);
                })
                .completionHandler(c -> consumerSetupCompletion.complete());
        return consumerSetupCompletion.future();
    }

    @Override
    public void stop() {
        String verticleName = getClass().getSimpleName();
        JsonObject message = new JsonObject().put("target", verticleName);

        vertx.eventBus().publish(ROUTE_REMOVE, message);
    }
}
