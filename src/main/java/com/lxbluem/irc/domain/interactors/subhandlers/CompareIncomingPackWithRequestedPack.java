package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompareIncomingPackWithRequestedPack implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    public static final Pattern PATTERN = Pattern.compile("\\*\\* sending you pack #\\d+ \\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE);

    public CompareIncomingPackWithRequestedPack(BotStorage botStorage, BotStateStorage stateStorage) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        AtomicBoolean handledThisCommand = new AtomicBoolean(false);

        botStorage.get(botNickName).ifPresent(ircBot ->
                stateStorage.get(botNickName)
                        .ifPresent(botState -> {
                            if (!remoteName.equalsIgnoreCase(botState.getRemoteUser()))
                                return;

                            Matcher matcher = PATTERN.matcher(noticeMessage);
                            if (!matcher.find())
                                return;
                            handledThisCommand.set(true);

                            String incomingPackName = matcher.group(1);

                            String packName = botState.getPack().getPackName();
                            if (!(incomingPackName.equalsIgnoreCase(packName))) {
                                ircBot.startSearchListing(remoteName, packName);
                                botState.requestSearchListing();
                                return;
                            }

                            botState.remoteSendsCorrectPack();
                        })
        );

        return handledThisCommand.get();
    }
}
