package com.lxbluem.filenameresolver.adapters;

import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystem;
import io.vertx.rxjava.core.file.FileProps;
import rx.Single;

import java.util.List;

public class FileSystemImpl implements FileSystem {
    private final io.vertx.rxjava.core.file.FileSystem vertxFileSystem;

    public FileSystemImpl(io.vertx.rxjava.core.file.FileSystem vertxFileSystem) {
        this.vertxFileSystem = vertxFileSystem;
    }

    @Override
    public Single<Boolean> fileOrDirExists(String path) {
        return vertxFileSystem.rxExists(path);
    }

    @Override
    public Single<Void> mkdir(String path) {
        return vertxFileSystem.rxMkdir(path);
    }

    @Override
    public Single<List<String>> readDir(String path) {
        return vertxFileSystem.rxReadDir(path);
    }

    @Override
    public Single<Long> fileSize(String path) {
        return vertxFileSystem.rxProps(path)
                .map(FileProps::size);
    }
}
