package com.lxbluem;

public enum Address {
    ROUTE_ADD("route"),
    ROUTE_REMOVE("unroute"),
    BOT_INITIALIZED("bot.init"),
    BOT_JOIN("bot.join"),
    BOT_FAILED("bot.fail"),
    DCC_INITIALIZE("bot.dcc.init"),
    DCC_REQUESTING("bot.dcc.request"),
    DCC_STARTED("bot.dcc.start"),
    DCC_PROGRESSED("bot.dcc.progress"),
    DCC_QUEUED("bot.dcc.queue"),
    DCC_FINISHED("bot.dcc.finish"),
    DCC_FAILED("bot.dcc.failed"),
    DCC_TERMINATE("bot.dcc.terminate"),
    FILENAME_RESOLVE("filename.resolve"),
    BOT_NOTICE("bot.notice"),
    BOT_NICK_UPDATED("bot.nick"),
    BOT_EXITED("bot.exit"),
    STATE("state"),
    REMOVED_STALE_BOTS("removed");

    private final String addressValue;

    Address(String addressValue) {
        this.addressValue = addressValue;
    }

    public String address() {
        return addressValue;
    }
}
