package com.lxbluem.filesystem;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class FilenameMemDb {

    private String path;
    private FileSystem fileSystem;
    private final FilenameMapper filenameMapper;
    private Set<FileEntity> filesOnDisk = new HashSet<>();

    public FilenameMemDb(Vertx vertx, String path, FilenameMapper filenameMapper) {
        this.path = path;
        fileSystem = vertx.fileSystem();
        this.filenameMapper = filenameMapper;

        prefillEntitySet();
    }

    private void prefillEntitySet() {
        Future<List<FileEntity>> fileEntitiesFuture = entitiesOnDisk();

        fileEntitiesFuture
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
            if (throwable instanceof FileSystemException) {
                return Future.failedFuture(throwable);
            }

            Future<Void> future = mkdir(path);
            return future.compose(c -> readDir());
        });
    }

    private Future<Boolean> dirExists() {
        Future<Boolean> exitsFuture = Future.future();
        fileSystem.exists(path, exitsFuture);
        return exitsFuture;
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
                        String relativeFilename = filename.substring(lastIndexOf, filename.length());

                        fileEntityFuture.complete(FileEntity.builder()
                                .packname(filenameMapper.getPackName(relativeFilename))
                                .size(fileProps.size())
                                .suffix(filenameMapper.getPackSuffix(relativeFilename))
                                .build()
                        );
                    } else {
                        fileEntityFuture.fail(filePropsFuture.cause());
                    }
                }
        );

        return fileEntityFuture;
    }

    public Future<List<FileEntity>> getPackFilesFromDisk(String packname) {
        List<FileEntity> fileEntities = filesOnDisk.stream()
                .filter(fe -> {
                    String fePackname = fe.getPackname();
                    String fePacknameSubstring = fePackname.substring(path.length() + 1);
                    return fePacknameSubstring.equalsIgnoreCase(packname);
                })
                .collect(toList());
        return Future.succeededFuture(fileEntities);
    }

    public Future<String> getPackFilesName(String requestedPackName) {
        List<FileEntity> fileEntities = filesOnDisk.stream()
                .filter(fe -> {
                    String fePackname = fe.getPackname();
                    String fePacknameSubstring = fePackname.substring(path.length() + 1);
                    String packName = filenameMapper.getPackName(fePacknameSubstring);
                    return fePacknameSubstring.equalsIgnoreCase(packName);
                })
                .collect(toList());

        Integer suffix = fileEntities.stream()
                .max(Comparator.comparingInt(FileEntity::getSuffix))
                .map(FileEntity::getSuffix)
                .orElse(0);

        String fsFilename = suffix > 0 ? filenameMapper.getFsFilename(requestedPackName, suffix + 1) : requestedPackName;
        return Future.succeededFuture(fsFilename);
    }
}
