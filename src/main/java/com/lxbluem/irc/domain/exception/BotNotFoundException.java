package com.lxbluem.irc.domain.exception;

public class BotNotFoundException extends RuntimeException {
    public BotNotFoundException(String botname) {
        super(String.format("bot '%s' not found", botname));
    }
}
