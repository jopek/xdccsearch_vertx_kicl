package com.lxbluem.irc;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.net.NetClient;
import io.vertx.rxjava.core.net.NetServer;
import io.vertx.rxjava.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static com.lxbluem.Addresses.*;

public class DccReceiverVerticle extends AbstractVerticle {
    private static Logger LOG = LoggerFactory.getLogger(DccReceiverVerticle.class);

    private EventBus eventBus;

    @Override
    public void start() throws Exception {
        eventBus = vertx.eventBus();

        eventBus.<JsonObject>consumer(BOT_DCC_INIT)
                .toObservable()
                .subscribe(handleMessage());
    }

    private Action1<Message<JsonObject>> handleMessage() {
        return event -> {
            Boolean passive = event.body().getBoolean("passive", false);
            LOG.info("type of transfer: {}", passive ? "passive" : "active");
            transferFile(event.body(), event::reply, passive);
        };
    }

    private void transferFile(JsonObject message, Handler<JsonObject> replyHandler, boolean passive) {
        if (!passive)
            transferFileActive(message);
        else
            transferFilePassive(message, replyHandler);
    }

    private void transferFilePassive(JsonObject message, Handler<JsonObject> replyHandler) {
        JsonObject pack = message.getJsonObject("pack");

        NetServerOptions netServerOptions = new NetServerOptions().setReceiveBufferSize(1 << 18);
        NetServer netServer = vertx.createNetServer(netServerOptions);
        Observable<NetSocket> socketObservable = netServer.connectStream().toObservable();

        subscribeTo(message, socketObservable);
        listenToIncomingDccRequests(replyHandler, pack, netServer);
    }

    private void transferFileActive(JsonObject message) {
        String host = message.getString("ip");
        Integer port = message.getInteger("port");

        NetClientOptions netClientOptions = new NetClientOptions().setReceiveBufferSize(1 << 18);
        NetClient netClient = vertx.createNetClient(netClientOptions);
        Observable<NetSocket> socketObservable = netClient.rxConnect(port, host).toObservable();

        subscribeTo(message, socketObservable);
    }


    private void listenToIncomingDccRequests(Handler<JsonObject> replyHandler, JsonObject pack, NetServer netServer) {
        netServer.rxListen(0)
                .toObservable()
                .subscribe(
                        server -> {
                            eventBus.publish(BOT_DCC_START, new JsonObject()
                                    .put("timestamp", Instant.now().toEpochMilli())
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

    private void subscribeTo(JsonObject message, Observable<NetSocket> socketObservable) {
        String filename = message.getString("filename");
        JsonObject pack = message.getJsonObject("pack");

        AtomicReference<File> file = new AtomicReference<>(new File(filename));
        AtomicReference<RandomAccessFile> fileOutput = new AtomicReference<>(null);
        try {
            fileOutput.set(new RandomAccessFile(file.get().getCanonicalPath(), "rw"));
            fileOutput.get().seek(0);
        } catch (IOException error) {
            LOG.error("error opening file before transfer", error);
            eventBus.publish(BOT_DCC_FAIL, new JsonObject()
                    .put("message", error.getMessage())
                    .put("timestamp", Instant.now().toEpochMilli())
                    .put("source", "connect")
                    .put("pack", pack)
            );
            return;
        }

        socketObservable.subscribe(
                socket -> {
                    eventBus.publish(BOT_DCC_START, new JsonObject()
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
                                    bufferReceivedAction(fileOutput.get(), pack, socket, outBuffer, bytesTransferredValue, lastProgressAt),
                                    errorAction(filename, pack),
                                    completedAction(filename, file.get(), fileOutput.get(), pack)
                            );
                },
                error -> {
                    eventBus.publish(BOT_DCC_FAIL, new JsonObject()
                            .put("message", error.getMessage())
                            .put("timestamp", Instant.now().toEpochMilli())
                            .put("source", "connect")
                            .put("pack", pack)
                    );
                    LOG.error("transfer of {} failed {}", filename, error.getMessage());
                }
        );
    }

    private Action1<Buffer> bufferReceivedAction(
            RandomAccessFile file,
            JsonObject pack,
            NetSocket netSocket,
            byte[] outBuffer,
            long[] bytesTransferedValue,
            long[] lastProgressAt
    ) {
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
            eventBus.publish(BOT_DCC_PROGRESS, new JsonObject()
                    .put("bytes", bytesTransfered)
                    .put("timestamp", nowInMillis)
                    .put("pack", pack)
            );
        };
    }

    private Action1<Throwable> errorAction(String filename, JsonObject pack) {
        return error -> {
            eventBus.publish(BOT_DCC_FAIL, new JsonObject()
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
            eventBus.publish(BOT_DCC_FINISH, new JsonObject()
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
