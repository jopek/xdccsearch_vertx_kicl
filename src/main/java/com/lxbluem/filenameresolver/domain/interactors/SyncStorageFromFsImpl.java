package com.lxbluem.filenameresolver.domain.interactors;

import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.incoming.SyncStorageFromFs;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileEntityStorage;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;

import java.util.List;
import java.util.stream.Collectors;

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
        List<String> directoryListing = fileSystem.readDir(downloadsPath)
                .stream()
                .map(canonicalPath -> {
                    int lastIndexOf = canonicalPath.lastIndexOf(downloadsPath);
                    if (lastIndexOf == -1)
                        return canonicalPath;
                    return canonicalPath.substring(lastIndexOf + downloadsPath.length() + 1);
                })
                .collect(Collectors.toList());
        List<FileEntity> allFileEntities = fileEntityStorage.getAll();

        allFileEntities.stream()
                .filter(fileEntity -> !directoryListing.contains(fileEntity.getFilenameOnDisk()))
                .forEach(fileEntityStorage::remove);

        allFileEntities
                .stream()
                .filter(FileEntity::isInUse)
                .filter(fileEntity -> directoryListing.contains(fileEntity.getFilenameOnDisk()))
                .forEach(fileEntity -> {
                    fileEntity.setInUse(false);
                    fileEntityStorage.save(fileEntity);
                });
    }
}
