package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.ScheduledTaskExecution;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.concurrent.TimeUnit;

public class AlreadyDownloadedNoticeMessageHandler implements NoticeMessageHandler.SubHandler {

    private final BotStorage botStorage;
    private final StateStorage stateStorage;
    private final EventDispatcher eventDispatcher;
    private final ScheduledTaskExecution scheduledTaskExecution;

    public AlreadyDownloadedNoticeMessageHandler(
            BotStorage botStorage,
            StateStorage stateStorage,
            EventDispatcher eventDispatcher,
            ScheduledTaskExecution scheduledTaskExecution) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
        this.scheduledTaskExecution = scheduledTaskExecution;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String noticeMessage = command.getNoticeMessage();

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (!lowerCaseNoticeMessage.contains("you already requested that pack")) {
            return false;
        }
        String eventMessage = noticeMessage + " - retrying in 1min";
        BotNoticeEvent noticeEvent = new BotNoticeEvent(botNickName, "", eventMessage);
        eventDispatcher.dispatch(noticeEvent);

        BotNoticeEvent retryingMessage = BotNoticeEvent.builder()
                .botNickName(botNickName)
                .remoteNick("")
                .noticeMessage("retrying to request pack")
                .build();

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {
                    Pack pack = state.getPack();
                    Runnable execution = () -> {
                        eventDispatcher.dispatch(retryingMessage);
                        bot.requestDccPack(pack.getNickName(), pack.getPackNumber());
                    };
                    scheduledTaskExecution.scheduleTask(botNickName, execution, 1L, TimeUnit.MINUTES);
                }));
        return true;
    }

}
