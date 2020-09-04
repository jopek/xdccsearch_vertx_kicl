package com.lxbluem.irc;

import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.domain.exception.DccTerminatedException;
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
import rx.Single;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static com.lxbluem.common.infrastructure.Address.*;

public class DccReceiverVerticle extends AbstractVerticle {
    private static final int PORT = 3400;
    private static final Logger LOG = LoggerFactory.getLogger(DccReceiverVerticle.class);

    private final BotMessaging botMessaging;
    private NetClient netClient;
    private NetServer netServer;

    public DccReceiverVerticle(BotMessaging botMessaging) {
        this.botMessaging = botMessaging;
    }

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();

        final int bufferSize = 1 << 18;
        netClient = vertx.createNetClient(new NetClientOptions().setReceiveBufferSize(bufferSize));
        netServer = vertx.createNetServer(new NetServerOptions().setReceiveBufferSize(bufferSize));

        PublishSubject<NetSocket> serverSocketSubject = PublishSubject.create();
        Action1<NetSocket> logConnection = netSocket -> LOG.info("SOCKET connect stream : l:{}:{} r:{}:{}",
                netSocket.remoteAddress().host(),
                netSocket.remoteAddress().port(),
                netSocket.localAddress().host(),
                netSocket.localAddress().port()
        );
        netServer.connectStream()
                .toObservable()
                .doOnNext(logConnection)
                .subscribe(serverSocketSubject);

        netServer.rxListen(PORT).subscribe(
                listeningNetServer -> LOG.info("listening on port {}", listeningNetServer.actualPort()),
                error -> LOG.error("{}", error.getMessage())
        );

        Observable<Message<JsonObject>> botExited = eventBus.<JsonObject>consumer(BOT_EXITED.address())
                .toObservable();

        Observable<Message<JsonObject>> botFailed = eventBus.<JsonObject>consumer(BOT_FAILED.address())
                .toObservable();

        Observable<String> terminationForBot =
                botExited.mergeWith(botFailed)
                        .map(Message::body)
                        .map(jso -> jso.getString("bot"))
                        .doOnNext(botNickName -> LOG.info("about to terminate DCC connection for {}", botNickName));

        eventBus.<JsonObject>consumer(DCC_INITIALIZE.address())
                .toObservable()
                .doOnNext(this::replyToSender)
                .map(Message::body)
                .groupBy(this::isPassiveTransfer)
                .flatMap(group -> {
                    // is passive
                    if (group.getKey())
                        return group.sample(serverSocketSubject)
                                .withLatestFrom(serverSocketSubject, (initMessage, netSocket) ->
                                        new InitMessageConnection(initMessage, Single.just(netSocket)));

                    // is active
                    return group.map(initMessage -> {
                        Single<NetSocket> netSocketSingle = getActiveTransferClient(initMessage);
                        return new InitMessageConnection(initMessage, netSocketSingle);
                    });
                })
                .subscribe(messageConnection ->
                        subscribeTo(messageConnection.initMessage, messageConnection.netSocketSingle, terminationForBot)
                );
    }


    private void replyToSender(Message<JsonObject> event) {
        Boolean isPassive = isPassiveTransfer(event.body());
        LOG.info("type of transfer: {}", isPassive ? "passive" : "active");

        JsonObject replyMessage = new JsonObject();
        if (isPassive)
            replyMessage.put("port", netServer.actualPort());
        event.reply(replyMessage);
    }

    private Boolean isPassiveTransfer(JsonObject event) {
        return event.getBoolean("passive", false);
    }

    private Single<NetSocket> getActiveTransferClient(JsonObject message) {
        String host = message.getString("ip");
        Integer port = message.getInteger("port");
        return netClient.rxConnect(port, host);
    }

    private void subscribeTo(JsonObject message, Single<NetSocket> socketObservable, Observable<String> terminationForBotRequest) {
        String filename = message.getString("filename");
        String botname = message.getString("bot");
        long filesize = message.getLong("size", 0L);

        AtomicReference<File> file = new AtomicReference<>(new File(filename));
        AtomicReference<RandomAccessFile> fileOutput = new AtomicReference<>(null);
        try {
            fileOutput.set(new RandomAccessFile(file.get().getCanonicalPath(), "rw"));
            fileOutput.get().seek(0);
        } catch (IOException error) {
            LOG.error("error opening file before transfer", error);
            botMessaging.notify(DCC_FAILED.address(), botname, error);
            return;
        }

        socketObservable.subscribe(
                socket -> {
                    final JsonObject extra = new JsonObject()
                            .put("filenameOnDisk", filename)
                            .put("bytesTotal", filesize);
                    botMessaging.notify(DCC_STARTED.address(), botname, extra);
                    LOG.info("starting transfer of {}", filename);

                    byte[] outBuffer = new byte[4];
                    final long[] bytesTransferredValue = {0};
                    final long[] totalBytesValue = {filesize};
                    final long[] lastProgressAt = {0};

                    Observable<String> terminationFilter = terminationForBotRequest.filter(botname::equalsIgnoreCase)
                            .map(botToTerminate -> {
                                throw new DccTerminatedException(botToTerminate);
                            });
                    socket.toObservable()
                            .takeUntil(terminationFilter)
                            .subscribe(
                                    bufferReceivedAction(fileOutput.get(), botname, socket, outBuffer, bytesTransferredValue, totalBytesValue, lastProgressAt),
                                    errorAction(filename, botname),
                                    completedAction(filename, file.get(), fileOutput.get(), botname, socket)
                            );
                },
                errorAction(filename, botname)
        );
    }

    private Action1<Buffer> bufferReceivedAction(
            RandomAccessFile file,
            String botname,
            NetSocket netSocket,
            byte[] outBuffer,
            long[] bytesTransferedValue,
            long[] totalBytesValue,
            long[] lastProgressAt
    ) {
        return buffer -> {
            long bytesTransfered = bytesTransferedValue[0];
            long totalBytes = totalBytesValue[0];
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
            if (nowInMillis <= lastProgress + 1000 && totalBytes - bytesTransfered > 0)
                return;

            lastProgressAt[0] = nowInMillis;
            botMessaging.notify(DCC_PROGRESSED.address(), botname, new JsonObject().put("bytes", bytesTransfered));
        };
    }

    private Action1<Throwable> errorAction(String filename, String botname) {
        return error -> {
            if (error instanceof DccTerminatedException) {
                botMessaging.notify(DCC_TERMINATED.address(), botname, error);
            } else {
                botMessaging.notify(DCC_FAILED.address(), botname, error);
            }
            LOG.error("transfer of {} failed: {}", filename, error.getMessage());
        };
    }

    private Action0 completedAction(String filename, File file, RandomAccessFile fileOutput, String botname, NetSocket socket) {
        return () -> {
            botMessaging.notify(DCC_FINISHED.address(), botname);
            LOG.info("transfer of {} finished", filename);

            try {
                fileOutput.close();
                socket.closeHandler((aVoid) -> LOG.info("completedAction, closing socket"))
                        .close();
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

    private static class InitMessageConnection {
        private final JsonObject initMessage;
        private final Single<NetSocket> netSocketSingle;

        InitMessageConnection(JsonObject initMessage, Single<NetSocket> netSocketSingle) {
            this.initMessage = initMessage;
            this.netSocketSingle = netSocketSingle;
        }
    }
}
