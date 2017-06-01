import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lxbluem.EventLogger;
import com.lxbluem.RouterVerticle;
import com.lxbluem.StatsVerticle;
import com.lxbluem.irc.BotVerticle;
import com.lxbluem.irc.ActiveDccReceiverVerticle;
import com.lxbluem.irc.PassiveDccReceiverVerticle;
import com.lxbluem.search.SearchVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;

public class Starter {
  public static void main(String[] args) throws InterruptedException {

    Json.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Vertx vertx = Vertx.vertx();
    deploy(vertx, EventLogger.class.getName());
    deploy(vertx, StatsVerticle.class.getName());
    deploy(vertx, ActiveDccReceiverVerticle.class.getName());
    deploy(vertx, PassiveDccReceiverVerticle.class.getName());

    deploy(vertx, RouterVerticle.class.getName(), event -> {
      System.out.println(RouterVerticle.class.getName() + " deployed " + event.result());
      deploy(vertx, SearchVerticle.class.getName());
      deploy(vertx, BotVerticle.class.getName());
    });
  }

  private static void deploy(Vertx vertx, String verticleClassname) {
    vertx.deployVerticle(verticleClassname, event ->
        System.out.println(verticleClassname + " deployed " + event.result()));
  }

  private static void deploy(Vertx vertx, String verticleClassname, Handler<AsyncResult<String>> asyncResultHandler) {
    vertx.deployVerticle(verticleClassname, asyncResultHandler);
  }
}
