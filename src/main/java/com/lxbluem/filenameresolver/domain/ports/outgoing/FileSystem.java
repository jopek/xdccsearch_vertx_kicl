package com.lxbluem.filenameresolver.domain.ports.outgoing;

import rx.Single;

import java.util.List;

public interface FileSystem {
    Single<Boolean> fileOrDirExists(String path);

    Single<Void> mkdir(String path);

    Single<List<String>> readDir(String path);

    Single<Long> fileSize(String path);
}
