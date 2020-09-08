package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.ChannelExtractor;
import com.lxbluem.irc.domain.model.request.JoinMentionedChannelsCommand;
import com.lxbluem.irc.domain.ports.incoming.JoinMentionedChannels;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.Set;

public class JoinMentionedChannelsImpl implements JoinMentionedChannels {
    private final BotStorage botStorage;
    private final StateStorage stateStorage;

    public JoinMentionedChannelsImpl(BotStorage botStorage, StateStorage stateStorage) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
    }

    @Override
    public void handle(JoinMentionedChannelsCommand command) {
        String botNickName = command.getBotNickName();
        String channelName = command.getChannelName();
        String topic = command.getTopic();

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {
                    Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(topic);
                    Set<String> channelReferences = state.channelReferences(channelName, mentionedChannels);
                    bot.joinChannel(channelReferences);
                })
        );
    }

}
