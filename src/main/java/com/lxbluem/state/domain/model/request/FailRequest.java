package com.lxbluem.state.domain.model.request;

import lombok.Data;

@Data
public class FailRequest {
    private final String botName;
    private final String message;
    private final long timestamp;
}
