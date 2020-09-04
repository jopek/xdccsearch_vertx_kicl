package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.domain.model.request.CtcpQueryCommand;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.ports.incoming.CtcpQueryHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.DCC_INITIALIZE;
import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static java.lang.String.format;

public class CtcpQueryHandlerImpl implements CtcpQueryHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final BotMessaging botMessaging;

    public CtcpQueryHandlerImpl(BotStorage botStorage, BotStateStorage stateStorage, BotMessaging botMessaging) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
    }

    @Override
    public void handle(CtcpQueryCommand command) {
        String botNickName = command.getBotNickName();
        DccCtcpQuery ctcpQuery = command.getCtcpQuery();
        long localIp = command.getLocalIp();

        if (!ctcpQuery.isValid()) {
            return;
        }
        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {

                    AtomicReference<String> resolvedFilename = new AtomicReference<>("");

                    Consumer<Map<String, Object>> passiveDccSocketPortConsumer = (answer) -> {
                        int passiveDccSocketPort = (int) answer.getOrDefault("port", 0);
                        if (passiveDccSocketPort == 0)
                            return;
                        String nickName = botState.getPack().getNickName();
                        String dccSendRequest = format("DCC SEND %s %d %d %d %d",
                                resolvedFilename.get(),
                                localIp,
                                passiveDccSocketPort,
                                ctcpQuery.getSize(),
                                ctcpQuery.getToken()
                        );

                        bot.sendCtcpMessage(nickName, dccSendRequest);
                    };

                    Consumer<Map<String, Object>> filenameResolverConsumer = (filenameAnswerMap) -> {
                        String filenameAnswer = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
                        resolvedFilename.set(filenameAnswer);
                        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNickName);
                        botMessaging.ask(DCC_INITIALIZE, query, passiveDccSocketPortConsumer);
                    };

                    botMessaging.ask(FILENAME_RESOLVE, new FilenameResolveRequest(ctcpQuery.getFilename()), filenameResolverConsumer);
                }));
    }
}
