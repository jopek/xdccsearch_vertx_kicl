package com.lxbluem;

import com.lxbluem.model.RouterRegistryMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

public abstract class AbstractRouteVerticle extends AbstractVerticle {

  protected void registerRoute(HttpMethod method, String route) {
    RouterRegistryMessage routerRegistryMessage = RouterRegistryMessage.builder()
        .method(method.name())
        .path(route)
        .target(getClass().getSimpleName())
        .build();

    vertx.eventBus()
        .publish("route", JsonObject.mapFrom(routerRegistryMessage));
  }

  @Override
  public void stop() throws Exception {
    String verticleName = getClass().getSimpleName();
    JsonObject message = new JsonObject().put("target", verticleName);

    vertx.eventBus().publish("unroute", message);
  }
}
