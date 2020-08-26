package com.lxbluem.common.infrastructure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SerializedRequest implements Serializable {
    private String method;
    private Map<String, String> params;
    private Map<String, String> headers;
    private String body;
}
