package com.lxbluem.common.infrastructure;

import com.lxbluem.common.domain.events.*;

public enum Address {
    ROUTE_ADD("route", DummyEvent.class),
    ROUTE_REMOVE("unroute", DummyEvent.class),
    BOT_INITIALIZED("bot.init", BotInitializedEvent.class),
    BOT_JOIN("bot.join", DummyEvent.class),
    BOT_FAILED("bot.fail", BotFailedEvent.class),
    DCC_INITIALIZE("bot.dcc.init", DummyEvent.class),
    DCC_REQUESTING("bot.notice", BotDccPackRequestedEvent.class),
    DCC_STARTED("bot.dcc.start", DummyEvent.class),
    DCC_PROGRESSED("bot.dcc.progress", DummyEvent.class),
    DCC_QUEUED("bot.dcc.queue", DccQueuedEvent.class),
    DCC_FINISHED("bot.dcc.finish", DummyEvent.class),
    DCC_FAILED("bot.dcc.failed", DummyEvent.class),
    DCC_TERMINATE("bot.dcc.terminate", DummyEvent.class),
    FILENAME_RESOLVE("filename.resolve", DummyEvent.class),
    BOT_NOTICE("bot.notice", BotNoticeEvent.class),
    BOT_NICK_UPDATED("bot.nick", BotRenamedEvent.class),
    BOT_EXITED("bot.exit", BotExitedEvent.class),
    STATE("state", DummyEvent.class),
    REMOVED_STALE_BOTS("removed", DummyEvent.class);

    private final String addressValue;
    private final Class<? extends Event> eventClass;

    Address(String addressValue, Class<? extends Event> eventClass) {
        this.addressValue = addressValue;
        this.eventClass = eventClass;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public String address() {
        return addressValue;
    }
}
