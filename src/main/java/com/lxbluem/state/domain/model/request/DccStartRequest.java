package com.lxbluem.state.domain.model.request;

import lombok.Data;

@Data
public class DccStartRequest {
    private final String botName;
    private final long bytesTotal;
    private final String filenameOnDisk;
    private final long timestamp;
}
