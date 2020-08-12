package com.lxbluem.state.domain.model.request;

import lombok.Data;

@Data
public class DccFinishRequest {
    private final String botName;
    private final long timestamp;

}
