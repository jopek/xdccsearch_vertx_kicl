package com.lxbluem.filenameresolver.domain.interactors;

import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;
import io.vertx.rxjava.core.file.FileProps;
import io.vertx.rxjava.core.file.FileSystem;

import java.util.List;

public class FileSystemBlockingImpl implements FileSystemBlocking {

    private final FileSystem vertxFileSystem;

    public FileSystemBlockingImpl(FileSystem vertxFileSystem) {
        this.vertxFileSystem = vertxFileSystem;
    }


    @Override
    public boolean fileOrDirExists(String path) {
        return vertxFileSystem.existsBlocking(path);
    }

    @Override
    public void mkdir(String path) {
        vertxFileSystem.mkdirBlocking(path);
    }

    @Override
    public List<String> readDir(String path) {
        return vertxFileSystem.readDirBlocking(path);
    }

    @Override
    public List<String> readDir(String path, String filter) {
        return vertxFileSystem.readDirBlocking(path, filter);
    }

    @Override
    public long fileSize(String path) {
        FileProps fileProps = vertxFileSystem.propsBlocking(path);
        return fileProps.size();
    }
}
