package com.lxbluem.irc;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.net.NetServer;

public class PassiveDccReceiverVerticle extends AbstractVerticle {

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("bot.dcc.init.passive")
                .toObservable()
                .subscribe(event -> {
                            JsonObject message = (JsonObject) event.body();
                            Handler<JsonObject> handler = event::reply;
                            transferFile(message, handler);
                        }
                );
    }

    private void transferFile(JsonObject message, Handler<JsonObject> replyHandler) {
        int buffersize = 1 << 15;
        JsonObject pack = (JsonObject) message.getValue("pack");

        NetServerOptions netServerOptions = new NetServerOptions().setReceiveBufferSize(buffersize);
        NetServer netServer = vertx.createNetServer(netServerOptions);

        netServer.connectHandler(socket -> {
            eventBus.publish("bot.dcc.start", new JsonObject()
                    .put("pack", pack)
            );

            byte[] outBuffer = new byte[4];
            final long[] bytesTransferedValue = {0};

            socket.toObservable()
                    .subscribe(
                            buffer -> {
                                long bytesTransfered = bytesTransferedValue[0];
                                bytesTransfered += buffer.length();
                                outBuffer[0] = (byte) ((bytesTransfered >> 24) & 0xff);
                                outBuffer[1] = (byte) ((bytesTransfered >> 16) & 0xff);
                                outBuffer[2] = (byte) ((bytesTransfered >> 8) & 0xff);
                                outBuffer[3] = (byte) (bytesTransfered & 0xff);
                                socket.write(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(outBuffer)));
                                bytesTransferedValue[0] = bytesTransfered;

                                eventBus.publish("bot.dcc.progress", new JsonObject()
                                        .put("bytes", bytesTransfered)
                                        .put("pack", pack)
                                );
                            },
                            error -> eventBus.publish("bot.dcc.fail", new JsonObject()
                                    .put("message", error.getMessage())
                                    .put("pack", pack)
                            ),
                            () -> eventBus.publish("bot.dcc.finish", new JsonObject()
                                    .put("pack", pack)
                            )
                    );
        });

        netServer.rxListen(0)
                .toObservable()
                .subscribe(
                        server -> {
                            eventBus.publish("bot.dcc.start", new JsonObject()
                                    .put("pack", pack)
                            );

                            replyHandler.handle(new JsonObject()
                                    .put("port", server.actualPort())
                            );
                        },

                        error ->
                                eventBus.publish("bot.dcc.fail", new JsonObject()
                                        .put("message", error.getMessage())
                                        .put("pack", pack)
                                )
                );
    }
}
