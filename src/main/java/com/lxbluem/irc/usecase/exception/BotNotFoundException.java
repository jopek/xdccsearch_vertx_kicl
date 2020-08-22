package com.lxbluem.irc.usecase.exception;

public class BotNotFoundException extends RuntimeException {
    public BotNotFoundException(String botname) {
        super(String.format("bot '%s' not found", botname));
    }
}
