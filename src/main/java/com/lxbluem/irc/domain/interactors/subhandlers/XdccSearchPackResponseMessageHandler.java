package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XdccSearchPackResponseMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final StateStorage stateStorage;
    private final EventDispatcher eventDispatcher;

    public XdccSearchPackResponseMessageHandler(BotStorage botStorage, StateStorage stateStorage, EventDispatcher eventDispatcher) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
    }

    // TODO check with tests once they work again
    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        IrcBot bot = botStorage.get(botNickName).orElseThrow();
        State state = stateStorage.get(botNickName).orElseThrow();

        if (!state.matchesRemoteUser(remoteName))
            return false;
        if (noticeMessage.toLowerCase().contains("list stopped")) {
            return true;
        }
        if (noticeMessage.toLowerCase().contains("searching for")) {
            return true;
        }
        if (!state.isSearchRequested())
            return false;

        /*
          [23:33]  -mybotDCCp- Searching for "test"...
          [23:33]  -mybotDCCp-  - Pack #1 matches, "test1.bin"
          [23:33]  -mybotDCCp-  - Pack #2 matches, "test2.bin"
         */
        Pattern pattern = Pattern.compile("- pack #(\\d+) matches, \"(.*?)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(noticeMessage);
        if (!matcher.find()) {
            return false;
        }

        bot.stopSearchListing(remoteName);
        state.stopSearchListing();

        String packNumberMatch = matcher.group(1);
        int incomingPackNumber = Integer.parseInt(packNumberMatch);
        int packNumber = state.getPack().getPackNumber();

        String incomingPackName = matcher.group(2);

        if (state.isSamePackNumber(incomingPackNumber)) {
            state.continueCtcpHandshake();
            return true;
        }
        bot.requestDccPack(remoteName, incomingPackNumber);
        state.updatePackCoreInformation(incomingPackNumber, incomingPackName);
        String message = String.format("pack number changed #%d -> #%d; requesting #%d", packNumber, incomingPackNumber, incomingPackNumber);
        eventDispatcher.dispatch(new BotNoticeEvent(botNickName, remoteName, message));
        return true;
    }

}
