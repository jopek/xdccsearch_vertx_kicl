package irc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import model.PackInfo;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Bot extends AbstractVerticle {
    public static final String ADDRESS = "bot";
    private EventBus eventBus;

    @Override
    public void start() {
        eventBus = vertx.eventBus();
        eventBus.consumer("bot.start", this::parseBotStartMessage);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/xfers")
                .handler(this::parseBotStartMessageByHttp);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080);
    }

    private void parseBotStartMessageByHttp(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        HttpServerRequest request = routingContext.request();
        int statusCode = response.getStatusCode();
        System.out.println("STATUS CODE: " + statusCode);

        Json.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        PackInfo packInfo = Json.decodeValue(routingContext.getBodyAsString(), PackInfo.class);

        System.out.println(packInfo);

        response.end();
    }

    private void parseBotStartMessage(Message<Object> objectMessage) {
        JsonObject body = (JsonObject) objectMessage.body();
        String nick = body.getString("nick");

        startBot(nick);
    }

    private void startBot(String nick) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Client client = Client.builder()
                .serverHost("192.168.99.100")
                .serverPort(6667)
                .secure(false)
                .nick(nick)
                .listenInput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[I] " + line))
                .listenOutput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[O] " + line))
                .listenException(Throwable::printStackTrace)
                .build();

        client.getEventManager().registerEventListener(this);

        client.addChannel("#download");
    }

    @Handler
    public void onJoin(ChannelJoinEvent event) {
        System.out.println(event);

        StringBuilder buf = new StringBuilder();
        event.getAffectedChannel().ifPresent(buf::append);
        JsonObject message = new JsonObject()
                .put("event", event.toString())
                .put("channel", buf.toString());
        eventBus.publish(ADDRESS, message);
    }

    @Handler
    public void onMessage(PrivateMessageEvent event) {
        System.out.println(event);
        eventBus.publish("bot.start", new JsonObject().put("nick", event.getMessage()));
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) {
        System.out.println(event);
    }

    @Handler
    public void onError(NickRejectedEvent event) {
        event.getClient().shutdown("bye!");
        eventBus.publish("bot", new JsonObject().put("error", "NickRejectedEvent " + event.getAttemptedNick()));
    }

}
