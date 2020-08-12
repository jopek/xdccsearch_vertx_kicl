package com.lxbluem.state.domain.model.request;

import lombok.Data;

@Data
public class RenameBotRequest {
    private final String botName;
    private final String newBotName;
    private final String message;
    private final long timestamp;
}
