package com.lxbluem.irc;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.net.NetClient;
import io.vertx.rxjava.core.net.NetServer;
import io.vertx.rxjava.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

import java.io.IOException;

import static com.lxbluem.Addresses.*;

public class DccReceiverVerticle extends AbstractVerticle {
    private static Logger LOG = LoggerFactory.getLogger(DccReceiverVerticle.class);

    private EventBus eventBus;
    private Common common;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();
        common = new Common(vertx, LOG);

        eventBus.<JsonObject>consumer(BOT_DCC_INIT)
                .toObservable()
                .subscribe(handleMessage());
    }

    private Action1<Message<JsonObject>> handleMessage() {
        return event -> {
            String transfer_type = event.body().getString("transfer_type");
            LOG.info("type of transfer: {}", transfer_type);
            boolean active = transfer_type.equalsIgnoreCase("active");
            try {
                transferFile(event.body(), event::reply, active);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private void transferFile(JsonObject message, Handler<JsonObject> replyHandler, boolean active) throws IOException {
        if (active)
            transferFileActive(message);
        else
            transferFilePassive(message, replyHandler);
    }

    private void transferFilePassive(JsonObject message, Handler<JsonObject> replyHandler) throws IOException {
        JsonObject pack = (JsonObject) message.getValue("pack");

        NetServerOptions netServerOptions = new NetServerOptions().setReceiveBufferSize(1 << 18);
        NetServer netServer = vertx.createNetServer(netServerOptions);
        Observable<NetSocket> socketObservable = netServer.connectStream().toObservable();

        common.subscribeTo(message, socketObservable);
        listenToIncomingDccRequests(replyHandler, pack, netServer);
    }

    private void transferFileActive(JsonObject message) throws IOException {
        String host = message.getString("ip");
        Integer port = message.getInteger("port");

        NetClientOptions netClientOptions = new NetClientOptions().setReceiveBufferSize(1 << 18);
        NetClient netClient = vertx.createNetClient(netClientOptions);
        Observable<NetSocket> socketObservable = netClient.rxConnect(port, host).toObservable();

        common.subscribeTo(message, socketObservable);
    }


    private void listenToIncomingDccRequests(Handler<JsonObject> replyHandler, JsonObject pack, NetServer netServer) {
        netServer.rxListen(0)
                .toObservable()
                .subscribe(
                        server -> {
                            eventBus.publish(BOT_DCC_START, new JsonObject()
                                    .put("source", "listen")
                                    .put("pack", pack)
                            );

                            replyHandler.handle(new JsonObject()
                                    .put("port", server.actualPort())
                            );
                        },

                        error ->
                                eventBus.publish(BOT_DCC_FAIL, new JsonObject()
                                        .put("source", "listen")
                                        .put("message", error.getMessage())
                                        .put("pack", pack)
                                )
                );
    }

}
