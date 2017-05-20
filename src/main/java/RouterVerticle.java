import com.lxbluem.model.SerializedRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RouterVerticle extends AbstractVerticle {

  private Map<String, AtomicInteger> verticleCounter = new HashMap<>();

  @Override
  public void start(Future<Void> startFuture) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(8080, event -> {
          if (event.succeeded()) {
            System.out.printf("listening on port %d\n", event.result().actualPort());
            startFuture.complete();
          } else {
            startFuture.fail(event.cause());
          }
        });

    vertx.eventBus()
        .consumer("route", message -> {
          setupRouter(router, message);
        });

//    vertx.eventBus()
//        .consumer("unroute", message -> {
//          unroute(router, message);
//        });

  }

  private void unroute(Router router, Message<Object> message) {
    System.out.println(">>> " + message.body());
    JsonObject newRouteMesage = (JsonObject) message.body();
    String target = newRouteMesage.getString("target");
    verticleCounter.get(target).decrementAndGet();
    publishVerticleRouteStats();
  }

  private void publishVerticleRouteStats() {
    vertx.eventBus().publish("router.stats", new JsonObject().put("verticles", verticleCounter));
  }

  void setupRouter(Router router, Message<Object> newRouteRequestMessage) {
    JsonObject messageBody = (JsonObject) newRouteRequestMessage.body();
    String path = messageBody.getString("path");
    String target = messageBody.getString("target");
    String method = messageBody.getString("method");

    HttpMethod httpMethod = HttpMethod.valueOf(method);

    verticleCounter.putIfAbsent(target, new AtomicInteger(0));
    verticleCounter.get(target).incrementAndGet();
    publishVerticleRouteStats();

    router.route(httpMethod, path).handler(rc -> {
      Map<String, String> params = new HashMap<>();
      rc.request().params().forEach(e -> params.put(e.getKey(), e.getValue()));

      Map<String, String> headers = new HashMap<>();
      rc.request().headers().forEach(e -> headers.put(e.getKey(), e.getValue()));

      SerializedRequest serializedRequest = SerializedRequest.builder()
          .method(method)
          .body(rc.getBody().toString())
          .headers(headers)
          .params(params)
          .build();

      vertx.eventBus()
          .send(target, JsonObject.mapFrom(serializedRequest), replyMessage -> {
            if (replyMessage.succeeded())
              sendHttpReply(rc, replyMessage);
            else
              rc.response()
                  .setStatusCode(400)
                  .setStatusMessage("somehow not successful")
                  .end();
          });
    });

  }

  private void sendHttpReply(RoutingContext rc, AsyncResult<Message<Object>> replyMessage) {
    Message<Object> result = replyMessage.result();
    JsonObject jsonObject = (JsonObject) result.body();
    Buffer buffer = Buffer.buffer(jsonObject.encode());

    rc.response().end(buffer);
  }

}
