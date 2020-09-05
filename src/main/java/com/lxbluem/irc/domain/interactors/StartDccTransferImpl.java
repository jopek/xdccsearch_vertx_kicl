package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.model.request.StartDccTransferCommand;
import com.lxbluem.irc.domain.ports.incoming.StartDccTransfer;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import lombok.extern.log4j.Log4j;

import java.util.Map;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.DCC_INITIALIZE;
import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static java.lang.String.format;

@Log4j
public class StartDccTransferImpl implements StartDccTransfer {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final BotMessaging botMessaging;

    public StartDccTransferImpl(BotStorage botStorage, BotStateStorage stateStorage, BotMessaging botMessaging) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
    }

    @Override
    public void handle(StartDccTransferCommand command) {
        String botNickName = command.getBotNickName();
        DccCtcpQuery ctcpQuery = command.getCtcpQuery();
        long localIp = command.getLocalIp();

        if (!ctcpQuery.isValid())
            return;

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {

                    String packName = botState.getPack().getPackName();
                    String incomingFilename = ctcpQuery.getFilename();
                    if (!incomingFilename.equalsIgnoreCase(packName)) {
                        String message = "incoming file '%s' does not match known pack file '%s'";
                        log.warn(String.format(message, incomingFilename, packName));
                        return;
                    }

                    Consumer<Map<String, Object>> passiveDccSocketPortConsumer = (answer) -> {
                        int passiveDccSocketPort = (int) answer.getOrDefault("port", 0);
                        if (passiveDccSocketPort == 0)
                            return;
                        String nickName = botState.getPack().getNickName();
                        String dccSendRequest = format("DCC SEND %s %d %d %d %d",
                                incomingFilename,
                                localIp,
                                passiveDccSocketPort,
                                ctcpQuery.getSize(),
                                ctcpQuery.getToken()
                        );

                        bot.sendCtcpMessage(nickName, dccSendRequest);
                    };

                    Consumer<Map<String, Object>> filenameResolverConsumer = (filenameAnswerMap) -> {
                        String resolvedFilename = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
                        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNickName, resolvedFilename);
                        botMessaging.ask(DCC_INITIALIZE, query, passiveDccSocketPortConsumer);
                    };

                    botMessaging.ask(FILENAME_RESOLVE, new FilenameResolveRequest(incomingFilename), filenameResolverConsumer);
                }));
    }
}
