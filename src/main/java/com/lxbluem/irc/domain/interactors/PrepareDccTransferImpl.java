package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.model.request.PrepareDccTransferCommand;
import com.lxbluem.irc.domain.ports.incoming.PrepareDccTransfer;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
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
    private final BotStateStorage stateStorage;
    private final BotMessaging botMessaging;

    public PrepareDccTransferImpl(BotStorage botStorage, BotStateStorage stateStorage, BotMessaging botMessaging) {
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

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {

                    String packName = botState.getPack().getPackName();
                    String incomingFilename = ctcpQuery.getFilename();
                    String packNickName = botState.getPack().getNickName();

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

                        bot.sendCtcpMessage(packNickName, dccSendRequest);
                    };

                    Consumer<Map<String, Object>> filenameResolverConsumer = (filenameAnswerMap) -> {
                        String resolvedFilename = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
                        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNickName, resolvedFilename);
                        botMessaging.ask(DCC_INITIALIZE, query, passiveDccSocketPortConsumer);
                    };

                    FilenameResolveRequest resolveRequest = new FilenameResolveRequest(incomingFilename);
                    botState.saveCtcpHandshake(
                            () -> botMessaging.ask(FILENAME_RESOLVE, resolveRequest, filenameResolverConsumer)
                    );
                    if (botState.isRemoteSendsCorrectPack())
                        botState.continueCtcpHandshake();
                })
        );
    }
}
