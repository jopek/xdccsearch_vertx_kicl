package com.lxbluem.stats;

import java.util.Date;

public enum BotState {
    START, PROGRESS, FINISH;

    private long timestamp;

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }


    @Override
    public String toString() {
        return super.toString() + "@" + new Date(timestamp);
    }
}
