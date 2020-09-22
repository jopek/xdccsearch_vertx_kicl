package com.lxbluem.filenameresolver.adapters;

import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileEntityStorage;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryEntityStorage implements FileEntityStorage {
    private final Set<FileEntity> filesOnDisk = new HashSet<>();

    @Override
    public void save(FileEntity fileEntity) {
        filesOnDisk.remove(fileEntity);
        filesOnDisk.add(fileEntity);
    }

    @Override
    public void saveAll(Collection<FileEntity> fileEntities) {
        filesOnDisk.addAll(fileEntities);
    }

    @Override
    public List<FileEntity> getAll() {
        return new ArrayList<>(filesOnDisk);
    }

    @Override
    public List<FileEntity> findByName(String filename) {
        return filesOnDisk.stream()
                .filter(fileEntity -> fileEntity.getPackname().equalsIgnoreCase(filename))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileEntity> findByNameAndSize(String filename, long filesize) {
        return filesOnDisk.stream()
                .filter(fileEntity -> fileEntity.getPackname().equalsIgnoreCase(filename))
                .filter(fileEntity -> fileEntity.getPacksize() == filesize)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FileEntity> findByFileName(String filename) {
        return filesOnDisk.stream()
                .filter(fileEntity -> fileEntity.getFilenameOnDisk().equalsIgnoreCase(filename))
                .findFirst();
    }

    @Override
    public void remove(FileEntity fileEntity) {
        filesOnDisk.remove(fileEntity);
    }

    @Override
    public void removeAll(Collection<FileEntity> toBeRemoved) {
        filesOnDisk.removeAll(toBeRemoved);
    }
}
