package com.lxbluem.common.infrastructure;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class RouterRegistryMessage implements Serializable {
    private String method;
    private String path;
    private String target;
}
