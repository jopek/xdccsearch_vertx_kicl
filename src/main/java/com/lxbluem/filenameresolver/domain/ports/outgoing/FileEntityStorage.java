package com.lxbluem.filenameresolver.domain.ports.outgoing;

import com.lxbluem.filenameresolver.domain.model.FileEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileEntityStorage {
    void save(FileEntity fileEntity);
    void saveAll(Collection<FileEntity> fileEntities);
    List<FileEntity> getAll();
    List<FileEntity> findByName(String filename);
    List<FileEntity> findByNameAndSize(String filename, long filesize);
    Optional<FileEntity> findByFileName(String filename);
    void remove(FileEntity fileEntity);
    void removeAll(Collection<FileEntity> toBeRemoved);
}
