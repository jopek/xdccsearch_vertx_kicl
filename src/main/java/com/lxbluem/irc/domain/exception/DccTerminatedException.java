package com.lxbluem.irc.domain.exception;

public class DccTerminatedException extends RuntimeException {
    public DccTerminatedException(String botname) {
        super(String.format("bot '%s' terminated", botname));
    }
}
