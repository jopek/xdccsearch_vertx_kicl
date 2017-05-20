import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import com.lxbluem.model.RouterRegistryMessage;

import static io.vertx.core.http.HttpMethod.GET;

public class ServiceVerticleB extends AbstractVerticle {

  @Override
  public void start() {
    EventBus eventBus = vertx.eventBus();
    registerRoute(eventBus);

    eventBus.consumer(getClass().getSimpleName(), this::readRequest);

    vertx.setTimer((long) 10e3, timeout ->
        vertx.undeploy(deploymentID(), x ->
            System.out.printf("undeploy succeeded? %s\n", x.succeeded())
        )
    );

  }

  private void registerRoute(EventBus eventBus) {
    RouterRegistryMessage routerRegistryMessage = RouterRegistryMessage.builder()
        .method(GET.name())
        .path("/b")
        .target(getClass().getSimpleName())
        .build();

    eventBus.publish("route", JsonObject.mapFrom(routerRegistryMessage));
  }

  private void readRequest(Message<Object> message) {
    System.out.println(">>> " + message.body());
    JsonObject reply = new JsonObject().put("data", deploymentID());
    message.reply(reply);
  }

  private void log(AsyncResult<Message<Object>> reply) {
    System.out.printf(
        "sender received reply %s from %s\n",
        reply.result().body(),
        reply.result().address()
    );
  }

}
