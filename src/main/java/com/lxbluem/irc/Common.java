package com.lxbluem.irc;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.net.NetSocket;
import org.slf4j.Logger;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;

class Common {
    private final EventBus eventBus;
    private final Logger LOG;

    Common(Vertx vertx, Logger logger) {
        eventBus = vertx.eventBus();
        LOG = logger;
    }

    void subscribeTo(JsonObject message, Observable<NetSocket> socketObservable) throws IOException {
        String filename = message.getString("filename");

        File file = new File(filename);
        RandomAccessFile fileOutput = new RandomAccessFile(file.getCanonicalPath(), "rw");
        fileOutput.seek(0);

        JsonObject pack = (JsonObject) message.getValue("pack");

        socketObservable.subscribe(
                socket -> {
                    eventBus.publish("bot.dcc.start", new JsonObject()
                            .put("source", "connect")
                            .put("pack", pack)
                            .put("timestamp", Instant.now().toEpochMilli())
                    );
                    LOG.info("starting transfer of {}", filename);

                    byte[] outBuffer = new byte[4];
                    final long[] bytesTransferredValue = {0};
                    final long[] lastProgressAt = {0};

                    socket.toObservable()
                            .subscribe(
                                    bufferReceivedAction(fileOutput, pack, socket, outBuffer, bytesTransferredValue, lastProgressAt),
                                    errorAction(filename, pack),
                                    completedAction(filename, file, fileOutput, pack)
                            );
                },
                error -> {
                    eventBus.publish("bot.dcc.fail", new JsonObject()
                            .put("message", error.getMessage())
                            .put("source", "connect")
                            .put("pack", pack)
                    );
                    LOG.error("transfer of {} failed {}", filename, error.getMessage());
                }
        );
    }

    private Action1<Buffer> bufferReceivedAction(RandomAccessFile file, JsonObject pack, NetSocket netSocket, byte[] outBuffer, long[] bytesTransferedValue, long[] lastProgressAt) {
        return buffer -> {
            long bytesTransfered = bytesTransferedValue[0];
            bytesTransfered += buffer.length();
            outBuffer[0] = (byte) ((bytesTransfered >> 24) & 0xff);
            outBuffer[1] = (byte) ((bytesTransfered >> 16) & 0xff);
            outBuffer[2] = (byte) ((bytesTransfered >> 8) & 0xff);
            outBuffer[3] = (byte) (bytesTransfered & 0xff);
            netSocket.write(Buffer.newInstance(io.vertx.core.buffer.Buffer.buffer(outBuffer)));
            bytesTransferedValue[0] = bytesTransfered;

            try {
                file.write(buffer.getDelegate().getBytes(), 0, buffer.length());
            } catch (IOException e) {
                e.printStackTrace();
            }

            long nowInMillis = Instant.now().toEpochMilli();
            long lastProgress = lastProgressAt[0];
            if (nowInMillis <= lastProgress + 1000)
                return;

            lastProgressAt[0] = nowInMillis;
            eventBus.publish("bot.dcc.progress", new JsonObject()
                    .put("bytes", bytesTransfered)
                    .put("timestamp", nowInMillis)
                    .put("pack", pack)
            );
        };
    }

    private Action1<Throwable> errorAction(String filename, JsonObject pack) {
        return error -> {
            eventBus.publish("bot.dcc.fail", new JsonObject()
                    .put("message", error.getMessage())
                    .put("source", "socket")
                    .put("pack", pack)
                    .put("timestamp", Instant.now().toEpochMilli())
            );
            LOG.error("transfer of {} failed {}", filename, error.getMessage());
        };
    }

    private Action0 completedAction(String filename, File file, RandomAccessFile fileOutput, JsonObject pack) {
        return () -> {
            eventBus.publish("bot.dcc.finish", new JsonObject()
                    .put("pack", pack)
                    .put("timestamp", Instant.now().toEpochMilli())
            );
            LOG.info("transfer of {} finished", filename);

            try {
                fileOutput.close();
            } catch (IOException e) {
                LOG.error("error closing file after transfer", e);
            }

            removePartExtension(file);
        };
    }

    private void removePartExtension(File file) {
        String filename = file.getPath().replace(".part", "");
        boolean renameSucceeded = file.renameTo(new File(filename));
    }
}
