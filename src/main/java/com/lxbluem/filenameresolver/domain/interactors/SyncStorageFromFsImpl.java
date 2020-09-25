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
                .map(this::basename)
                .collect(Collectors.toList());
        List<FileEntity> allFileEntities = fileEntityStorage.getAll();

        List<FileEntity> toRemove = allFileEntities.stream()
                .filter(fileEntity -> !directoryListing.contains(fileEntity.getFilenameOnDisk()))
                .collect(Collectors.toList());
        fileEntityStorage.removeAll(toRemove);

        List<FileEntity> toAdd = allFileEntities
                .stream()
                .filter(FileEntity::isInUse)
                .filter(fileEntity -> directoryListing.contains(fileEntity.getFilenameOnDisk()))
                .peek(fileEntity -> fileEntity.setInUse(false))
                .collect(Collectors.toList());
        fileEntityStorage.saveAll(toAdd);
    }

    private String basename(String canonicalPath) {
        int lastIndexOf = canonicalPath.lastIndexOf("/");
        if (lastIndexOf == -1)
            return canonicalPath;
        return canonicalPath.substring(lastIndexOf + 1);
    }
}
