package com.lxbluem.irc;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.net.NetClient;
import io.vertx.rxjava.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;

public class ActiveDccReceiverVerticle extends AbstractVerticle {

    private static Logger LOG = LoggerFactory.getLogger(ActiveDccReceiverVerticle.class);

    private Common common;

    @Override
    public void start() throws Exception {
        EventBus eventBus = vertx.eventBus();
        common = new Common(vertx, LOG);

        eventBus.consumer("bot.dcc.init.active")
                .toObservable()
                .subscribe(event -> {
                            JsonObject message = (JsonObject) event.body();
                            try {
                                transferFile(message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    private void transferFile(JsonObject message) throws IOException {
        String host = message.getString("ip");
        Integer port = message.getInteger("port");

        NetClientOptions netClientOptions = new NetClientOptions().setReceiveBufferSize(1 << 18);
        NetClient netClient = vertx.createNetClient(netClientOptions);
        Observable<NetSocket> socketObservable = netClient.rxConnect(port, host)
                .toObservable();

        common.subscribeTo(message, socketObservable);
    }

}
