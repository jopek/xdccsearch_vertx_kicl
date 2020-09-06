package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NoticeMessageHandlerImplTest {

    private EventDispatcher eventDispatcher;
    private NoticeMessageHandler noticeMessageHandler;

    @Before
    public void setUp() {
        eventDispatcher = mock(EventDispatcher.class);
        Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        noticeMessageHandler = new NoticeMessageHandlerImpl(eventDispatcher, clock);
    }

    @Test
    public void use_subhandler_skips_handlers_eventdispatcher() {
        noticeMessageHandler.registerMessageHandler(command -> false);
        noticeMessageHandler.registerMessageHandler(command -> true);

        noticeMessageHandler.handle(new NoticeMessageCommand("botNick", "remoteNick", "noticeMessage"));

        verify(eventDispatcher, never()).dispatch(any(Event.class));
    }

    @Test
    public void skip_other_subhandlers_if_previous_subhandler_handled_command() {
        Set<Integer> checkpoint = new HashSet<>();
        noticeMessageHandler.registerMessageHandler(command -> {
            checkpoint.add(1);
            return false;
        });
        noticeMessageHandler.registerMessageHandler(command -> {
            checkpoint.add(2);
            return false;
        });
        noticeMessageHandler.registerMessageHandler(command -> {
            checkpoint.add(3);
            return true;
        });
        noticeMessageHandler.registerMessageHandler(command -> {
            checkpoint.add(4);
            return false;
        });

        noticeMessageHandler.handle(new NoticeMessageCommand("botNick", "remoteNick", "noticeMessage"));

        assertTrue(checkpoint.containsAll(asList(1, 2, 3)));
        assertFalse(checkpoint.contains(4));
    }

    @Test
    public void notice_message_handler_send_other_messages_via_notice() {
        String botNick = "Andy";
        String remoteNick = "someDude";
        String noticeMessage = "lalala";

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        ArgumentCaptor<BotNoticeEvent> captor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(eventDispatcher).dispatch(captor.capture());
        BotNoticeEvent botNoticeEvent = captor.getValue();
        assertEquals("someDude", botNoticeEvent.getRemoteNick());
        assertEquals("lalala", botNoticeEvent.getMessage());
        verifyNoMoreInteractions(eventDispatcher);
    }
}