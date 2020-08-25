package com.lxbluem;

public enum Address {
    ROUTE_ADD("route"),
    ROUTE_REMOVE("unroute"),
    BOT_INIT("bot.init"),
    BOT_JOIN("bot.join"),
    BOT_FAIL("bot.fail"),
    BOT_DCC_INIT("bot.dcc.init"),
    BOT_DCC_REQUESTING("bot.dcc.request"),
    BOT_DCC_START("bot.dcc.start"),
    BOT_DCC_PROGRESS("bot.dcc.progress"),
    BOT_DCC_QUEUE("bot.dcc.queue"),
    BOT_DCC_FINISH("bot.dcc.finish"),
    BOT_DCC_FAILED("bot.dcc.failed"),
    BOT_DCC_TERMINATE("bot.dcc.terminate"),
    FILENAME_RESOLVE("filename.resolve"),
    BOT_NOTICE("bot.notice"),
    BOT_UPDATE_NICK("bot.nick"),
    BOT_EXIT("bot.exit"),
    STATE("state"),
    REMOVED_STALE_BOTS("removed");

    private final String addressValue;

    Address(String addressValue) {
        this.addressValue = addressValue;
    }

    public String getAddressValue() {
        return addressValue;
    }
}
