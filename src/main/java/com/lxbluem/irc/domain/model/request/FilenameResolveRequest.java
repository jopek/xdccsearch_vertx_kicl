package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class FilenameResolveRequest implements Serializable {
    private final String filename;
    private final long filesize;
}
