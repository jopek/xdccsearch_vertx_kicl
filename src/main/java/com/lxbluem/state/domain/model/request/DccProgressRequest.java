package com.lxbluem.state.domain.model.request;

import lombok.Data;

@Data
public class DccProgressRequest {
    private final String botName;
    private final long bytes;
    private final long timestamp;
}
