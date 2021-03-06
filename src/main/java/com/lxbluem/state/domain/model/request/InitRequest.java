package com.lxbluem.state.domain.model.request;

import com.lxbluem.common.domain.Pack;
import lombok.Data;

@Data
public class InitRequest {
    private final String botName;
    private final long timestamp;
    private final Pack pack;
}
