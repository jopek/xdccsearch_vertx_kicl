package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XdccSearchPackResponseMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;

    public XdccSearchPackResponseMessageHandler(BotStorage botStorage, BotStateStorage stateStorage, EventDispatcher eventDispatcher, Clock clock) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        AtomicBoolean conditionApplied = new AtomicBoolean(false);

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {
                    if (!remoteName.equalsIgnoreCase(botState.getRemoteUser()))
                        return;
                    if (noticeMessage.toLowerCase().contains("list stopped")) {
                        conditionApplied.set(true);
                        return;
                    }
                    if (noticeMessage.toLowerCase().contains("searching for")) {
                        conditionApplied.set(true);
                        return;
                    }

                    if (!botState.isSearchRequested())
                        return;

                    conditionApplied.set(true);

                        /*
                          [23:33]  -mybotDCCp- Searching for "test"...
                          [23:33]  -mybotDCCp-  - Pack #1 matches, "test1.bin"
                          [23:33]  -mybotDCCp-  - Pack #2 matches, "test2.bin"
                         */
                    Pattern pattern = Pattern.compile("- pack #(\\d+) matches, \"(.*?)\"", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(noticeMessage);
                    if (matcher.find()) {
                        bot.stopSearchListing(remoteName);
                        botState.stopSearchListing();

                        String packNumberMatch = matcher.group(1);
                        int incomingPackNumber = Integer.parseInt(packNumberMatch);
                        int packNumber = botState.getPack().getPackNumber();

                        String incomingPackName = matcher.group(2);

                        if (incomingPackNumber == packNumber)
                            botState.continueCtcpHandshake();
                        else {
                            bot.requestDccPack(remoteName, incomingPackNumber);
                            botState.getPack().setPackNumber(incomingPackNumber);
                            botState.getPack().setPackName(incomingPackName);
                            String message = String.format("pack number changed #%d -> #%d; requesting #%d", packNumber, incomingPackNumber, incomingPackNumber);
                            eventDispatcher.dispatch(new BotNoticeEvent(botNickName, nowEpochMillis(), remoteName, message));
                        }
                    }
                })
        );
        return conditionApplied.get();
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
