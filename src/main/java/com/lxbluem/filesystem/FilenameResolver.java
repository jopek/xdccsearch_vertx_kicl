package com.lxbluem.filesystem;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class FilenameResolver {

    private final String path;
    private final FileSystem fileSystem;
    private final FilenameMapper filenameMapper;
    private final Set<FileEntity> filesOnDisk = new HashSet<>();

    public FilenameResolver(Vertx vertx, String path, FilenameMapper filenameMapper) {
        this.path = path;
        fileSystem = vertx.fileSystem();
        this.filenameMapper = filenameMapper;

        prefillEntitySet();
    }

    private void prefillEntitySet() {
        entitiesOnDisk()
                .setHandler(all -> {
                    if (all.succeeded()) {
                        List<FileEntity> result = all.result();
                        filesOnDisk.clear();
                        filesOnDisk.addAll(result);
                    } else {
                        all.cause().printStackTrace();
                    }
                });
    }

    private Future<List<FileEntity>> entitiesOnDisk() {
        return getDirectoryListing().compose(filenames -> {
            List<Future> fileEntityFutures = filenames.stream()
                    .map(this::mapToFileEntityFuture)
                    .collect(toList());

            return CompositeFuture.all(fileEntityFutures)
                    .map(v -> fileEntityFutures.stream()
                            .map(f -> (FileEntity) f.result())
                            .collect(toList())
                    );
        });
    }

    private Future<List<String>> getDirectoryListing() {
        return dirExists().compose(exists -> {
            if (exists) {
                return readDir();
            }
            return Future.failedFuture(new FileSystemException(path + " does not exist"));
        }).recover(throwable -> {
            if (!(throwable instanceof FileSystemException)) {
                return Future.failedFuture(throwable);
            }

            Future<Void> future = mkdir(path);
            return future.compose(c -> readDir());
        });
    }

    private Future<Boolean> dirExists() {
        Promise<Boolean> exists = Promise.promise();
        fileSystem.exists(path, exists.future());
        return exists.future();
    }

    private Future<Void> mkdir(String path) {
        Future<Void> future = Future.future();
        fileSystem.mkdir(path, future);
        return future;
    }

    private Future<List<String>> readDir() {
        Future<List<String>> readDir = Future.future();
        fileSystem.readDir(path, readDir);
        return readDir;
    }


    private Future<FileEntity> mapToFileEntityFuture(String filename) {
        Future<FileProps> filePropsFuture = Future.future();
        fileSystem.props(filename, filePropsFuture);

        Future<FileEntity> fileEntityFuture = Future.future();
        filePropsFuture.setHandler(ar -> {
                    if (ar.succeeded()) {
                        FileProps fileProps = filePropsFuture.result();
                        int lastIndexOf = filename.lastIndexOf(path);

                        String relativeFilename;
                        if (lastIndexOf < 0) relativeFilename = filename;
                        else relativeFilename = filename.substring(lastIndexOf + path.length() + 1);

                        fileEntityFuture.complete(FileEntity.builder()
                                .packname(filenameMapper.createPackName(relativeFilename))
                                .path(path)
                                .size(fileProps.size())
                                .suffix(filenameMapper.createPackSuffix(relativeFilename))
                                .build()
                        );
                    } else {
                        fileEntityFuture.fail(filePropsFuture.cause());
                    }
                }
        );

        return fileEntityFuture;
    }

    public Future<String> getFileNameForPackName(String requestedPackName) {
        List<FileEntity> fileEntities = filesOnDisk.stream()
                .filter(fileEntityOnDisk -> fileEntityOnDisk.getPackname()
                        .equalsIgnoreCase(requestedPackName)
                )
                .collect(toList());

        int suffix = getSuffix(fileEntities);
        filesOnDisk.add(new FileEntity(requestedPackName, path, suffix, 0, 0, 0));
        String fsFilename = filenameMapper.getFsFilename(path + "/" + requestedPackName, suffix);
        return Future.succeededFuture(fsFilename);
    }

    private int getSuffix(List<FileEntity> fileEntities) {
        int suffix = fileEntities
                .stream()
                .max(Comparator.comparingInt(FileEntity::getSuffix))
                .map(FileEntity::getSuffix)
                .orElse(0);

        if (!fileEntities.isEmpty())
            suffix++;
        return suffix;
    }
}
