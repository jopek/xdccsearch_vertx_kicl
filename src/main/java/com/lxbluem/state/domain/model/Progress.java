package com.lxbluem.state.domain.model;

public record Progress(long size, long time) {

    public Progress(Progress other) {
        this(other.size, other.time);
    }
}
