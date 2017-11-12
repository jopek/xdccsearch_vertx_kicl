package com.lxbluem.model;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SerializedRequest implements Serializable {
    private String method;
    private Map<String, String> params;
    private Map<String, String> headers;
    private String body;
}
