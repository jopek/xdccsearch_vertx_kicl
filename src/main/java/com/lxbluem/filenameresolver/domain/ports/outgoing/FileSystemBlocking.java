package com.lxbluem.filenameresolver.domain.ports.outgoing;

import java.util.List;

public interface FileSystemBlocking {
    Boolean fileOrDirExists(String path);

    void mkdir(String path);

    List<String> readDir(String path);

    List<String> readDir(String path, String filter);

    long fileSize(String path);
}
