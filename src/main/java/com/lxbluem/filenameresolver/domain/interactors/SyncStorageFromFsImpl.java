package com.lxbluem.filenameresolver.domain.interactors;

import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.incoming.SyncStorageFromFs;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileEntityStorage;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;

import java.util.List;

public class SyncStorageFromFsImpl implements SyncStorageFromFs {
    private final FileSystemBlocking fileSystem;
    private final FileEntityStorage fileEntityStorage;
    private final String downloadsPath;

    public SyncStorageFromFsImpl(FileSystemBlocking fileSystem, FileEntityStorage fileEntityStorage, String downloadsPath) {
        this.fileSystem = fileSystem;
        this.fileEntityStorage = fileEntityStorage;
        this.downloadsPath = downloadsPath;
    }

    @Override
    public void execute() {
        ensureDestinationDirectoryExists();
        List<String> directoryListing = fileSystem.readDir(downloadsPath)
                .stream()
                .map(this::basename)
                .toList();
        List<FileEntity> allFileEntities = fileEntityStorage.getAll();

        List<FileEntity> toRemove = allFileEntities.stream()
                .filter(fileEntity -> !directoryListing.contains(fileEntity.getFilenameOnDisk()))
                .toList();
        fileEntityStorage.removeAll(toRemove);

        List<FileEntity> toAdd = allFileEntities
                .stream()
                .filter(FileEntity::isInUse)
                .filter(fileEntity -> directoryListing.contains(fileEntity.getFilenameOnDisk()))
                .toList();
        toAdd.forEach(fileEntity -> fileEntity.setInUse(false));

        fileEntityStorage.saveAll(toAdd);
    }

    private void ensureDestinationDirectoryExists() {
        boolean fileOrDirExists = fileSystem.fileOrDirExists(downloadsPath);
        if (fileOrDirExists)
            return;
        fileSystem.mkdir(downloadsPath);
    }

    private String basename(String canonicalPath) {
        int lastIndexOf = canonicalPath.lastIndexOf("/");
        if (lastIndexOf == -1)
            return canonicalPath;
        return canonicalPath.substring(lastIndexOf + 1);
    }
}
