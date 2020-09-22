package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendingYouPackNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final StateStorage stateStorage;
    public static final Pattern PATTERN = Pattern.compile("\\*\\* sending you pack #\\d+ \\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE);

    public SendingYouPackNoticeMessageHandler(BotStorage botStorage, StateStorage stateStorage) {
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
                        .ifPresent(state -> {
                            if (!remoteName.equalsIgnoreCase(state.getRemoteUser()))
                                return;

                            Matcher matcher = PATTERN.matcher(noticeMessage);
                            if (!matcher.find())
                                return;
                            handledThisCommand.set(true);

                            String incomingPackName = matcher.group(1);
                            if (noticeMessage.toLowerCase().contains("resume supported"))
                                state.packIsResumable();

                            String packName = state.getPack().getPackName();
                            if (!(incomingPackName.equalsIgnoreCase(packName))) {
                                ircBot.startSearchListing(remoteName, packName);
                                state.requestSearchListing();
                                return;
                            }

                            state.remoteSendsCorrectPack();
                        })
        );

        return handledThisCommand.get();
    }
}
