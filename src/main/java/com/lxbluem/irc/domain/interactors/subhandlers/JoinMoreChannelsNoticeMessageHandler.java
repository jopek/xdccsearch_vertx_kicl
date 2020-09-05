package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lxbluem.irc.domain.ChannelExtractor.getMentionedChannels;

public class JoinMoreChannelsNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;

    public JoinMoreChannelsNoticeMessageHandler(BotStorage botStorage, BotStateStorage stateStorage) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();
        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();

        AtomicBoolean conditionApplied = new AtomicBoolean(false);

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {

                    String channelName = botState.getPack().getChannelName();
                    if (lowerCaseNoticeMessage.contains(channelName.toLowerCase())) {
                        Set<String> mentionedChannels = getMentionedChannels(noticeMessage);
                        Set<String> references = botState.channelReferences(channelName, mentionedChannels);
                        bot.joinChannel(references);
                        conditionApplied.set(true);
                    }
                })
        );

        return conditionApplied.get();
    }
}
