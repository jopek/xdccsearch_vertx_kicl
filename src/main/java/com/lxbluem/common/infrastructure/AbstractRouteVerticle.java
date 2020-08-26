package com.lxbluem.common.infrastructure;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.BiConsumer;

import static com.lxbluem.common.infrastructure.Address.ROUTE_ADD;
import static com.lxbluem.common.infrastructure.Address.ROUTE_REMOVE;
import static java.lang.String.format;

public abstract class AbstractRouteVerticle extends AbstractVerticle {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    protected Future<Void> promisedRegisterRouteWithHandler(
            HttpMethod httpMethod,
            String route,
            BiConsumer<SerializedRequest, Promise<JsonObject>> requestHandler) {

        String target = format("%s:%s:%s", getClass().getSimpleName(), httpMethod, route);
        JsonObject routerRegistryMessageJsonObject = new JsonObject()
                .put("method", httpMethod.name())
                .put("path", route)
                .put("target", target);

        vertx.eventBus().publish(ROUTE_ADD.address(), routerRegistryMessageJsonObject);

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

        vertx.eventBus().publish(ROUTE_REMOVE.address(), message);
    }
}
