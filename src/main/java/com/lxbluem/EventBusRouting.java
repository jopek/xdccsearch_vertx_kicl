package com.lxbluem;

import com.lxbluem.model.RouterRegistryMessage;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static com.lxbluem.Address.ROUTE_ADD;
import static com.lxbluem.Address.ROUTE_REMOVE;
import static java.lang.String.format;

public class EventBusRouting {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final EventBus eventBus;

    public EventBusRouting(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void registerRoute(
            HttpMethod httpMethod,
            String route,
            Class<? extends AbstractVerticle> verticleClass,
            BiConsumer<SerializedRequest, Future<JsonObject>> requestHandler) {

        String target = format("%s:%s:%s", verticleClass.getSimpleName(), httpMethod, route);
        RouterRegistryMessage routerRegistryMessage = RouterRegistryMessage.builder()
                .method(httpMethod.name())
                .path(route)
                .target(target)
                .build();

        eventBus.publish(ROUTE_ADD.address(), JsonObject.mapFrom(routerRegistryMessage));

        eventBus.consumer(target, message -> {
            SerializedRequest request = Json.decodeValue(message.body().toString(), SerializedRequest.class);

            Future<JsonObject> future = Future.future();
            future.compose(message::reply, Future.future().setHandler(e ->
                    message.fail(500, e.cause().getMessage())
            ));

            requestHandler.accept(request, future);
        });
    }

    public void unregisterRoute() {
        String verticleName = getClass().getSimpleName();
        JsonObject message = new JsonObject().put("target", verticleName);

        eventBus.publish(ROUTE_REMOVE.address(), message);
    }
}
