package com.lxbluem.irc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;

public class PassiveDccReceiverVerticle extends AbstractVerticle {

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("bot.dcc.init.passive", event -> {
            JsonObject message = (JsonObject) event.body();
            Handler<JsonObject> handler = event::reply;
            transferFile(message, handler);
        });
    }

    private void transferFile(JsonObject message, Handler<JsonObject> replyHandler) {
        System.out.println("PASSIVE DCC TRANSFER");
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


            socket.handler(buffer -> {
                long bytesTransfered = bytesTransferedValue[0];

                bytesTransfered += buffer.getBytes().length;

                //Send back an acknowledgement of how many bytes we have got so far.
                //Convert bytesTransfered to an "unsigned, 4 byte integer in network byte order", per DCC specification
                outBuffer[0] = (byte) ((bytesTransfered >> 24) & 0xff);
                outBuffer[1] = (byte) ((bytesTransfered >> 16) & 0xff);
                outBuffer[2] = (byte) ((bytesTransfered >> 8) & 0xff);
                outBuffer[3] = (byte) (bytesTransfered & 0xff);
                socket.write(Buffer.buffer(outBuffer));
                bytesTransferedValue[0] = bytesTransfered;

                eventBus.publish("bot.dcc.progress", new JsonObject()
                        .put("bytes", bytesTransfered)
                        .put("pack", pack)
                );
            });

            socket.exceptionHandler(event -> eventBus.publish("bot.dcc.fail", new JsonObject()
                    .put("message", event.getMessage())
                    .put("pack", pack)
            ));

            socket.endHandler(end -> eventBus.publish("bot.dcc.finish", new JsonObject()
                    .put("pack", pack)
            ));
        });

        netServer.listen(0, res -> {
            if (res.succeeded()) {
                eventBus.publish("bot.dcc.start", new JsonObject()
                        .put("pack", pack)
                );

                replyHandler.handle(new JsonObject()
                        .put("port", res.result().actualPort())
                );

            } else {
                eventBus.publish("bot.dcc.fail", new JsonObject()
                        .put("message", res.cause().getMessage())
                        .put("pack", pack)
                );
            }
        });
    }
}
