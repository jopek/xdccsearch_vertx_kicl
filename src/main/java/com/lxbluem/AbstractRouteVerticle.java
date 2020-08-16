package com.lxbluem;

import com.lxbluem.model.RouterRegistryMessage;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
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

    protected void registerRouteWithHandler(
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

        vertx.eventBus().consumer(target, message -> {
            SerializedRequest request = Json.decodeValue(message.body().toString(), SerializedRequest.class);

            Future future = Future.future(message::reply)
                    .onFailure(e -> message.fail(500, e.getCause().getMessage()));

            requestHandler.accept(request, future);
        });
    }

    @Override
    public void stop() throws Exception {
        String verticleName = getClass().getSimpleName();
        JsonObject message = new JsonObject().put("target", verticleName);

        vertx.eventBus().publish(ROUTE_REMOVE, message);
    }
}
