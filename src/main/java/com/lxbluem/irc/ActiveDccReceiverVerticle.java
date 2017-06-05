package com.lxbluem.irc;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.net.NetClient;

import java.time.Instant;

public class ActiveDccReceiverVerticle extends AbstractVerticle {

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("bot.dcc.init.active")
                .toObservable()
                .subscribe(event -> {
                            JsonObject message = (JsonObject) event.body();
                            transferFile(message);
                        }
                );
    }

    private void transferFile(JsonObject message) {
        //        RandomAccessFile fileOutput = new RandomAccessFile(file.getCanonicalPath(), "rw");
//        fileOutput.seek(startPosition);
        String host = message.getString("ip");
        JsonObject pack = (JsonObject) message.getValue("pack");

        int buffersize = 1 << 18;

        NetClientOptions netClientOptions = new NetClientOptions().setReceiveBufferSize(buffersize);
        NetClient netClient = vertx.createNetClient(netClientOptions);
        netClient.rxConnect(message.getInteger("port"), host)
                .toObservable()
                .subscribe(
                        netSocket -> {
                            eventBus.publish("bot.dcc.start", new JsonObject()
                                    .put("pack", pack)
                                    .put("timestamp", Instant.now().toEpochMilli())
                            );

                            byte[] outBuffer = new byte[4];
                            final long[] bytesTransferedValue = {0};

                            netSocket.toObservable()
                                    .subscribe(
                                            buffer -> {
                                                long bytesTransfered = bytesTransferedValue[0];
                                                bytesTransfered += buffer.length();
                                                outBuffer[0] = (byte) ((bytesTransfered >> 24) & 0xff);
                                                outBuffer[1] = (byte) ((bytesTransfered >> 16) & 0xff);
                                                outBuffer[2] = (byte) ((bytesTransfered >> 8) & 0xff);
                                                outBuffer[3] = (byte) (bytesTransfered & 0xff);
                                                netSocket.write(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(outBuffer)));
                                                bytesTransferedValue[0] = bytesTransfered;

                                                eventBus.publish("bot.dcc.progress", new JsonObject()
                                                        .put("bytes", bytesTransfered)
                                                        .put("bufferBytes", buffer.length())
                                                        .put("timestamp", Instant.now().toEpochMilli())
                                                        .put("pack", pack)
                                                );
                                            },
                                            error ->
                                                    eventBus.publish("bot.dcc.fail", new JsonObject()
                                                            .put("message", error.getMessage())
                                                            .put("pack", pack)
                                                            .put("timestamp", Instant.now().toEpochMilli())
                                                    ),
                                            () ->
                                                    eventBus.publish("bot.dcc.finish", new JsonObject()
                                                            .put("pack", pack)
                                                            .put("timestamp", Instant.now().toEpochMilli())
                                                    )
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
