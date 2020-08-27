package com.lxbluem.common.infrastructure;

import com.lxbluem.common.domain.events.*;

public enum Address {
    ROUTE_ADD("route", Event.class),
    ROUTE_REMOVE("unroute", Event.class),
    BOT_INITIALIZED("bot.init", BotInitializedEvent.class),
    BOT_JOIN("bot.join", Event.class),
    BOT_FAILED("bot.fail", BotFailedEvent.class),
    DCC_INITIALIZE("bot.dcc.init", Event.class),
    DCC_REQUESTING("bot.dcc.request", Event.class),
    DCC_STARTED("bot.dcc.start", Event.class),
    DCC_PROGRESSED("bot.dcc.progress", Event.class),
    DCC_QUEUED("bot.dcc.queue", DccQueuedEvent.class),
    DCC_FINISHED("bot.dcc.finish", Event.class),
    DCC_FAILED("bot.dcc.failed", Event.class),
    DCC_TERMINATE("bot.dcc.terminate", Event.class),
    FILENAME_RESOLVE("filename.resolve", Event.class),
    BOT_NOTICE("bot.notice", BotNoticeEvent.class),
    BOT_NICK_UPDATED("bot.nick", BotRenamedEvent.class),
    BOT_EXITED("bot.exit", BotExitedEvent.class),
    STATE("state", Event.class),
    REMOVED_STALE_BOTS("removed", Event.class);

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
