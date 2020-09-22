package com.lxbluem.filenameresolver.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode(exclude = "inUse")
public class FileEntity {
    private String packname;
    private long packsize;
    private String filenameOnDisk;
    private boolean inUse;

    public FileEntity(String packname, long packsize, String filenameOnDisk, boolean inUse) {
        this.packname = packname;
        this.packsize = packsize;
        this.filenameOnDisk = filenameOnDisk;
        this.inUse = inUse;
    }
}
