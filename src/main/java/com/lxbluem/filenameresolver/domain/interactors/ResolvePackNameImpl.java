package com.lxbluem.filenameresolver.domain.interactors;

import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.incoming.ResolvePackName;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileEntityStorage;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;
import rx.Single;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class ResolvePackNameImpl implements ResolvePackName {

    private final FilenameMapper filenameMapper;
    private final FileSystemBlocking fs;
    private final FileEntityStorage storage;
    private final String downloadsPath;

    public ResolvePackNameImpl(
            FilenameMapper filenameMapper,
            FileSystemBlocking fileSystem,
            FileEntityStorage storage,
            String downloadsPath) {
        this.filenameMapper = filenameMapper;
        this.fs = fileSystem;
        this.storage = storage;
        this.downloadsPath = downloadsPath;
    }

    @Override
    public Single<Response> execute(String requestedFilename, long requestedFileSize) {
        List<String> packFilesOnDisk = fs.readDir(downloadsPath)
                .stream()
                .filter(f -> filenameMapper.createPackName(basename(f))
                        .equalsIgnoreCase(requestedFilename)
                )
                .toList();

        Map<String, Long> fileSizes = packFilesOnDisk.stream()
                .collect(toMap(
                        Function.identity(),
                        fs::fileSize
                ));

        List<String> partials = packFilesOnDisk.stream()
                .filter(f -> f.endsWith(".part"))
                .toList();

        List<String> complete = packFilesOnDisk.stream()
                .filter(f -> !f.endsWith(".part"))
                .filter(packFileOnDisk -> fileSizes.get(packFileOnDisk) == requestedFileSize)
                .toList();

        final int suffix = packFilesOnDisk
                .stream()
                .map(filenameMapper::createPackSuffix)
                .reduce(0, Math::max);


        if (!complete.isEmpty()) {
            String fullPath = complete.get(complete.size() - 1);
            String path = targetName(basename(fullPath));
            Response response = new Response(path, fileSizes.get(fullPath), true);
            return Single.just(response);
        }

        if (!partials.isEmpty()) {
            Map<String, FileEntity> fileEntityMap = storage
                    .findByNameAndSize(requestedFilename, requestedFileSize)
                    .stream()
                    .collect(toMap(
                            FileEntity::getFilenameOnDisk,
                            Function.identity()
                    ));


            List<String> unusedPartials = partials.stream()
                    .filter(packFileOnDisk -> !fileEntityMap.getOrDefault(basename(packFileOnDisk), FileEntity.builder()
                            .inUse(false)
                            .build()).isInUse())
                    .sorted(Comparator.comparingLong(fileSizes::get).reversed())
                    .toList();

            Response response;
            String fsFilename;
            if (unusedPartials.isEmpty()) {
                fsFilename = filenameMapper.getFsFilename(requestedFilename, suffix + 1);
                String path = targetName(fsFilename);
                response = new Response(path, fileSizes.getOrDefault(fsFilename, 0L), false);
            } else {
                String fullPath = unusedPartials.get(0);
                fsFilename = basename(fullPath);
                String path = targetName(fsFilename);
                response = new Response(path, fileSizes.getOrDefault(fullPath, 0L), false);
            }

            storage.save(new FileEntity(requestedFilename, requestedFileSize, fsFilename, true));

            return Single.just(response);
        }

        int useSuffix = (packFilesOnDisk.isEmpty()) ? suffix : suffix + 1;
        String fsFilename = filenameMapper.getFsFilename(requestedFilename, useSuffix);
        String path = targetName(fsFilename);
        Response response = new Response(path, 0, false);

        storage.save(new FileEntity(requestedFilename, requestedFileSize, fsFilename, true));

        return Single.just(response);
    }

    private String targetName(String fsFilename) {
        return String.format("%s/%s", downloadsPath, fsFilename);
    }

    private String basename(String canonicalPath) {
        int lastIndexOf = canonicalPath.lastIndexOf("/");
        if (lastIndexOf == -1)
            return canonicalPath;
        return canonicalPath.substring(lastIndexOf + 1);
    }
}

