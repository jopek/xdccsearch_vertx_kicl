package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.model.request.PrepareDccTransferCommand;
import com.lxbluem.irc.domain.ports.incoming.PrepareDccTransfer;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.DCC_INITIALIZE;
import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static java.lang.String.format;

public class PrepareDccTransferImpl implements PrepareDccTransfer {
    private static final Logger log = LoggerFactory.getLogger(PrepareDccTransferImpl.class);
    private final BotStorage botStorage;
    private final StateStorage stateStorage;
    private final BotMessaging botMessaging;

    public PrepareDccTransferImpl(BotStorage botStorage, StateStorage stateStorage, BotMessaging botMessaging) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
    }

    @Override
    public void handle(PrepareDccTransferCommand command) {
        String botNickName = command.getBotNickName();
        DccCtcpQuery ctcpQuery = command.getCtcpQuery();
        long localIp = command.getLocalIp();

        log.debug("{}", ctcpQuery);

        if (!ctcpQuery.isValid())
            return;

        /*
            in  DCC SEND <filename> <ip> 0 <filesize> <token>
            out DCC RESUME <filename> 0 <position> <token>
            in  DCC ACCEPT <filename> 0 <position> <token>
            out DCC SEND <filename> <peer-ip> <port> <filesize> <token>
         */

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {

                    String incomingFilename = ctcpQuery.getFilename();
                    String packNickName = state.getPack().getNickName();

                    Consumer<Map<String, Object>> passiveDccSocketPortConsumer = (answer) -> {
                        int passiveDccSocketPort = (int) answer.getOrDefault("port", 0);
                        if (passiveDccSocketPort == 0)
                            return;
                        String dccSendRequest = format("DCC SEND %s %d %d %d %d",
                                incomingFilename,
                                localIp,
                                passiveDccSocketPort,
                                ctcpQuery.getSize(),
                                ctcpQuery.getToken()
                        );

                        // DCC RESUME <filename> <port> <position>
                        long position = 0;
                        String dccResumeRequest = format("DCC RESUME %s %d %d",
                                incomingFilename,
                                passiveDccSocketPort,
                                position
                        );

                        bot.sendCtcpMessage(packNickName, dccSendRequest);
                    };

                    Consumer<Map<String, Object>> filenameResolverConsumer = (filenameAnswerMap) -> {
                        String resolvedFilename = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
                        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNickName, resolvedFilename);
                        botMessaging.ask(DCC_INITIALIZE, query, passiveDccSocketPortConsumer);
                    };

                    FilenameResolveRequest resolveRequest = new FilenameResolveRequest(incomingFilename, ctcpQuery.getSize());
                    state.saveCtcpHandshake(
                            () -> botMessaging.ask(FILENAME_RESOLVE, resolveRequest, filenameResolverConsumer)
                    );
                    if (state.isRemoteSendsCorrectPack())
                        state.continueCtcpHandshake();
                })
        );
    }
}
