package com.lxbluem.irc;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.net.NetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;

public class PassiveDccReceiverVerticle extends AbstractVerticle {
    private static Logger LOG = LoggerFactory.getLogger(PassiveDccReceiverVerticle.class);

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("bot.dcc.init.passive")
                .toObservable()
                .subscribe(event -> {
                            JsonObject message = (JsonObject) event.body();
                            Handler<JsonObject> handler = event::reply;
                            try {
                                transferFile(message, handler);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    private void transferFile(JsonObject message, Handler<JsonObject> replyHandler) throws IOException {
        String filename = message.getString("filename");

        File file = new File(filename);
        RandomAccessFile fileOutput = new RandomAccessFile(file.getCanonicalPath(), "rw");
        fileOutput.seek(0);

        int buffersize = 1 << 18;
        JsonObject pack = (JsonObject) message.getValue("pack");

        NetServerOptions netServerOptions = new NetServerOptions().setReceiveBufferSize(buffersize);
        NetServer netServer = vertx.createNetServer(netServerOptions);

        netServer.connectHandler(socket -> {
            eventBus.publish("bot.dcc.start.connect", new JsonObject()
                    .put("pack", pack)
            );
            LOG.info("starting transfer of {}", filename);

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

                                try {
                                    fileOutput.write(buffer.getDelegate().getBytes(), 0, buffer.length());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                eventBus.publish("bot.dcc.progress", new JsonObject()
                                        .put("bytes", bytesTransfered)
                                        .put("bufferBytes", buffer.length())
                                        .put("timestamp", Instant.now().toEpochMilli())
                                        .put("pack", pack)
                                );
                            },
                            error -> {
                                eventBus.publish("bot.dcc.fail.socket", new JsonObject()
                                        .put("message", error.getMessage())
                                        .put("pack", pack)
                                );
                                LOG.error("transfer of {} failed {}", filename, error);
                            },
                            () -> {
                                eventBus.publish("bot.dcc.finish", new JsonObject()
                                        .put("pack", pack)
                                );
                                LOG.info("transfer of {} finished", filename);

                                try {
                                    fileOutput.close();
                                } catch (IOException e) {
                                    LOG.error("error closing file after transfer", e);
                                }

                                removePartExtension(file);
                            }
                    );
        });

        netServer.rxListen(0)
                .toObservable()
                .subscribe(
                        server -> {
                            eventBus.publish("bot.dcc.start.listen", new JsonObject()
                                    .put("pack", pack)
                            );

                            replyHandler.handle(new JsonObject()
                                    .put("port", server.actualPort())
                            );
                        },

                        error ->
                                eventBus.publish("bot.dcc.fail.listen", new JsonObject()
                                        .put("message", error.getMessage())
                                        .put("pack", pack)
                                )
                );
    }

    private void removePartExtension(File file) {
        String filename = file.getPath().replace(".part", "");
        file.renameTo(new File(filename));
    }
}
