package com.lxbluem.filesystem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FileEntity {
    private String name;
    private String rndsuffix;
    private long size;
    private long packsize;
    private long createdAt;
}
