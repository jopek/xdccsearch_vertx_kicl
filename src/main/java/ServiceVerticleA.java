import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

public class ServiceVerticleA extends AbstractVerticle {

  @Override
  public void start() {

    EventBus eventBus = vertx.eventBus();

    eventBus.publish(
        "route",
        new JsonObject()
            .put("method", GET.name())
            .put("path", "/a")
            .put("target", getClass().getSimpleName())
    );

    eventBus.publish(
        "route",
        new JsonObject()
            .put("method", GET.name())
            .put("path", "/a/:part1/:part2")
            .put("target", getClass().getSimpleName())
    );

    eventBus.publish(
        "route",
        new JsonObject()
            .put("method", POST.name())
            .put("path", "/a")
            .put("target", getClass().getSimpleName())
    );


    eventBus.consumer(getClass().getSimpleName(), this::readRequest);


  }

  private void readRequest(Message<Object> message) {
    System.out.println(">>> " + message.body());
    JsonObject reply = new JsonObject().put("data", deploymentID());

    message.reply(reply);
  }

}
