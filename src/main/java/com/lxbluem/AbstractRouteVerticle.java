package com.lxbluem;

import com.lxbluem.model.RouterRegistryMessage;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.function.BiConsumer;

public abstract class AbstractRouteVerticle extends AbstractVerticle {

  protected void registerRouteWithHandler(String address, HttpMethod httpMethod, String route, BiConsumer<SerializedRequest, Future<JsonObject>> requestHandler) {
    RouterRegistryMessage routerRegistryMessage = RouterRegistryMessage.builder()
        .method(httpMethod.name())
        .path(route)
        .target(getClass().getSimpleName())
        .build();

    vertx.eventBus().publish("route", JsonObject.mapFrom(routerRegistryMessage));

    vertx.eventBus().consumer(address, message -> {
      SerializedRequest request = Json.decodeValue(message.body().toString(), SerializedRequest.class);

      Future<JsonObject> future = Future.future();
      future.compose(message::reply, Future.future().setHandler(e ->
          message.fail(500, e.cause().getMessage())
      ));

      requestHandler.accept(request, future);
    });
  }

  @Override
  public void stop() throws Exception {
    String verticleName = getClass().getSimpleName();
    JsonObject message = new JsonObject().put("target", verticleName);

    vertx.eventBus().publish("unroute", message);
  }
}
