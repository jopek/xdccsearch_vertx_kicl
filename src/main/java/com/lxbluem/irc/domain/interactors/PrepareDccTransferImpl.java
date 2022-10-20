package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.domain.model.request.CtcpDccSend;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.DccResumeAcceptTransferCommand;
import com.lxbluem.irc.domain.model.request.DccSendTransferCommand;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.model.request.ReasonedExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.PrepareDccTransfer;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.DCC_INITIALIZE;
import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static com.lxbluem.irc.domain.model.request.CtcpDccSend.TransferType.ACTIVE;
import static com.lxbluem.irc.domain.model.request.CtcpDccSend.TransferType.PASSIVE;
import static java.lang.String.format;

public class PrepareDccTransferImpl implements PrepareDccTransfer {
    private static final Logger log = LoggerFactory.getLogger(PrepareDccTransferImpl.class);
    private final BotStorage botStorage;
    private final StateStorage stateStorage;
    private final BotMessaging botMessaging;
    private final ExitBot exitBot;

    public PrepareDccTransferImpl(BotStorage botStorage, StateStorage stateStorage, BotMessaging botMessaging, ExitBot exitBot) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
        this.exitBot = exitBot;
    }

    @Override
    public void handle(DccSendTransferCommand command) {
        String botNickName = command.getBotNickName();
        CtcpDccSend ctcpQuery = command.getCtcpQuery();
        long localIp = command.getLocalIp();

        log.debug("{}", ctcpQuery);

        if (!ctcpQuery.isValid())
            return;

        /*
            dcc send:
             in  DCC SEND   <filename> <sender-ip> <sender-port> <filesize>
            [out DCC RESUME <filename> <sender-port> <position> <token>]
            [in  DCC ACCEPT <filename> <sender-port> <position> <token>]
            -> connect

            reverse dcc send:
             in  DCC SEND   <filename> <sender-ip> 0 <filesize> <token>
            [out DCC RESUME <filename> 0 <position> <token>]
            [in  DCC ACCEPT <filename> 0 <position> <token>]
             out DCC SEND <filename> <receiver-ip> <receiver-port> <filesize> <token>
            -> receive connection
         */

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {

                    String packNickName = state.getPack().getNickName();
                    AtomicLong positionInPartialDownload = new AtomicLong();

                    Consumer<Map<String, Object>> filenameResolverConsumer = filenameAnswerMap -> {
                        String resolvedFilename = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
                        positionInPartialDownload.set((Long) filenameAnswerMap.getOrDefault("position", 0L));
                        boolean isComplete = (Boolean) filenameAnswerMap.getOrDefault("isComplete", false);

                        state.resolvedFilename(resolvedFilename);
                        state.resolvedFilePartialSize(positionInPartialDownload.get());

                        if (isComplete) {
                            String format = format("file already downloaded as '%s'", resolvedFilename);
                            ReasonedExitCommand exitCommand = new ReasonedExitCommand(botNickName, format);
                            exitBot.handle(exitCommand);
                            return;
                        }

                        if (positionInPartialDownload.get() < ctcpQuery.getSize()) {
                            state.initialDccSendQuery(ctcpQuery);
                            long position = positionInPartialDownload.get();
                            // DCC RESUME <filename> <port> <position>
                            String dccResumeRequest = format("DCC RESUME %s %d %d%s",
                                    ctcpQuery.getFilename(),
                                    ctcpQuery.getPort(),
                                    position,
                                    ctcpQuery.getPort() == 0 ? String.format(" %d", ctcpQuery.getToken()) : ""
                            );
                            bot.sendCtcpMessage(packNickName, dccResumeRequest);
                            return;
                        }

                        DccInitializeRequest request = DccInitializeRequest.from(ctcpQuery, botNickName, resolvedFilename, 0L);

                        if (ctcpQuery.getTransferType() == ACTIVE)
                            botMessaging.ask(DCC_INITIALIZE, request, ignoreResponse());
                        if (ctcpQuery.getTransferType() == PASSIVE)
                            botMessaging.ask(DCC_INITIALIZE, request, finishReverseDccSend(bot, ctcpQuery, localIp, packNickName));
                    };

                    FilenameResolveRequest resolveRequest = new FilenameResolveRequest(ctcpQuery.getFilename(), ctcpQuery.getSize());
                    state.saveCtcpHandshake(
                            () -> botMessaging.ask(FILENAME_RESOLVE, resolveRequest, filenameResolverConsumer)
                    );
                    if (state.isRemoteSendsCorrectPack())
                        state.continueCtcpHandshake();
                })
        );
    }

    private Consumer<Map<String, Object>> ignoreResponse() {
        return unused -> {
        };
    }

    private Consumer<Map<String, Object>> finishReverseDccSend(IrcBot bot, CtcpDccSend ctcpQuery, long localIp, String packNickName) {
        return answer -> {
            int passiveDccSocketPort = (int) answer.getOrDefault("port", 0);
            String dccSendRequest = format("DCC SEND %s %d %d %d %d",
                    ctcpQuery.getFilename(),
                    localIp,
                    passiveDccSocketPort,
                    ctcpQuery.getSize(),
                    ctcpQuery.getToken()
            );

            bot.sendCtcpMessage(packNickName, dccSendRequest);
        };
    }

    @Override
    public void handle(DccResumeAcceptTransferCommand command) {
        String botNickName = command.getBotNickName();
        long localIp = command.getLocalIp();

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {
                    String resolvedFilename = state.getResolvedFilename();
                    long position = state.getResolvedFilePartialSize();
                    String packBotNickName = state.getPack().getNickName();
                    CtcpDccSend dccSend = state.getInitialDccSendQuery();
                    DccInitializeRequest query = DccInitializeRequest.from(dccSend, botNickName, resolvedFilename, position);

                    if (dccSend.getTransferType() == ACTIVE)
                        botMessaging.ask(DCC_INITIALIZE, query, ignoreResponse());
                    if (dccSend.getTransferType() == PASSIVE)
                        botMessaging.ask(DCC_INITIALIZE, query, finishReverseDccSend(bot, dccSend, localIp, packBotNickName));
                })
        );
    }
}
