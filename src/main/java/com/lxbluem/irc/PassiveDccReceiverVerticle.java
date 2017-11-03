package com.lxbluem.irc;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.net.NetServer;
import io.vertx.rxjava.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;

public class PassiveDccReceiverVerticle extends AbstractVerticle {
    private static Logger LOG = LoggerFactory.getLogger(PassiveDccReceiverVerticle.class);

    private EventBus eventBus;
    private Common common;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();
        common = new Common(vertx, LOG);

        eventBus.<JsonObject>consumer("bot.dcc.init.passive")
                .toObservable()
                .subscribe(event -> {
                            try {
                                transferFile(event.body(), event::reply);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    private void transferFile(JsonObject message, Handler<JsonObject> replyHandler) throws IOException {
        JsonObject pack = (JsonObject) message.getValue("pack");

        NetServerOptions netServerOptions = new NetServerOptions().setReceiveBufferSize(1 << 18);
        NetServer netServer = vertx.createNetServer(netServerOptions);
        Observable<NetSocket> socketObservable = netServer.connectStream()
                .toObservable();

        common.subscribeTo(message, socketObservable);
        listenToIncomingDccRequests(replyHandler, pack, netServer);
    }

    private void listenToIncomingDccRequests(Handler<JsonObject> replyHandler, JsonObject pack, NetServer netServer) {
        netServer.rxListen(0)
                .toObservable()
                .subscribe(
                        server -> {
                            eventBus.publish("bot.dcc.start", new JsonObject()
                                    .put("source", "listen")
                                    .put("pack", pack)
                            );

                            replyHandler.handle(new JsonObject()
                                    .put("port", server.actualPort())
                            );
                        },

                        error ->
                                eventBus.publish("bot.dcc.fail", new JsonObject()
                                        .put("source", "listen")
                                        .put("message", error.getMessage())
                                        .put("pack", pack)
                                )
                );
    }

}
