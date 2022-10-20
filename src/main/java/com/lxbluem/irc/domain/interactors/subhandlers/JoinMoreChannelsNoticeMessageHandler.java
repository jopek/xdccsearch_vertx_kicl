package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lxbluem.irc.domain.ChannelExtractor.getMentionedChannels;

public class JoinMoreChannelsNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final StateStorage stateStorage;

    public JoinMoreChannelsNoticeMessageHandler(BotStorage botStorage, StateStorage stateStorage) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String noticeMessage = command.getNoticeMessage();
        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();

        AtomicBoolean conditionApplied = new AtomicBoolean(false);

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {

                    String channelName = state.getPack().getChannelName();
                    if (lowerCaseNoticeMessage.contains(channelName.toLowerCase())) {
                        Set<String> mentionedChannels = getMentionedChannels(noticeMessage);
                        Set<String> references = state.channelReferences(channelName, mentionedChannels);
                        bot.joinChannel(references);
                        conditionApplied.set(true);
                    }
                })
        );

        return conditionApplied.get();
    }
}
