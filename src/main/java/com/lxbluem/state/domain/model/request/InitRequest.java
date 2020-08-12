package com.lxbluem.state.domain.model.request;

import com.lxbluem.domain.Pack;
import lombok.Data;

@Data
public class InitRequest {
    private final String botName;
    private final Pack pack;
}
