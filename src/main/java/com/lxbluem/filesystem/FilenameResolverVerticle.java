package com.lxbluem.filesystem;

import com.lxbluem.filesystem.repository.SimpleCrudRepository;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.file.FileProps;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class FilenameResolverVerticle extends AbstractVerticle {
    private static final String PATH = "downloads";

    private SimpleCrudRepository<FileEntity, FileEntity> repository;

    public static void main(String[] args) {
        Launcher.main(new String[]{"run", FilenameResolverVerticle.class.getName()});
    }

    @Override
    public void start() {
//        vertx.eventBus().consumer("filename.resolve", handler -> {
//            JsonObject body = (JsonObject) handler.body();
//            String requestedFilename = body.getString("filename");
//        });

        Future<Boolean> exitsFuture = getDirExists();

        exitsFuture.compose(exists -> {
            if (exists) {
                return getReadDirFuture();
            }
            return Future.failedFuture(PATH + " does not exist");
        }).recover(throwable -> {
            System.out.println("EEEEEEK AN EXCEPTION: " + throwable.getClass() + " " + throwable.getMessage());
            if (throwable instanceof RuntimeException) {
                return Future.failedFuture(throwable);
            }

            Future<Void> future = getMkdirFuture();
            return future.compose(c -> getReadDirFuture());
        }).compose(strings -> {
            System.out.println(strings);
            List<Future> collect = strings.stream().map(this::mapToFileEntity).collect(toList());
            return CompositeFuture.all(collect);
        }).setHandler(all -> {
            if (all.succeeded()) {
                System.out.println("SUCCCESSSSS");
                CompositeFuture result = all.result();
                result.list().forEach(System.out::println);
            } else {
                System.out.println("ERRORRRRRR");
                all.cause().printStackTrace();
            }
        });


//        Single<Void> rxMkdir = vertx.fileSystem().rxMkdir(PATH);

//        rxReadDir.doOnError(throwable -> rxMkdir.subscribe())
//                .retry()
//                .map(strings -> strings.stream()
//                        .map(this::mapToFileEntity)
//                        .collect(toList())
//                )
//                .subscribe(strings -> {
//                    System.out.println(strings);
//                });
    }

    private Future<Void> getMkdirFuture() {
        Future<Void> future = Future.future();
        vertx.fileSystem().mkdir(PATH, future.completer());
        return future;
    }

    private Future<List<String>> getReadDirFuture() {
        Future<List<String>> readDir = Future.future();
        vertx.fileSystem().readDir(PATH, readDir);
        return readDir;
    }

    private Future<Boolean> getDirExists() {
        Future<Boolean> exitsFuture = Future.future();
        vertx.fileSystem().exists(PATH, exitsFuture.completer());
        return exitsFuture;
    }

    private Future<FileEntity> mapToFileEntity(String str) {
        Future<FileProps> filePropsFuture = Future.future();
        vertx.fileSystem().props(str + "", filePropsFuture.completer());

        Future<FileEntity> fileEntityFuture = Future.future();
        filePropsFuture.setHandler(ar -> {
                    if (ar.succeeded()) {
                        System.out.printf("horray for the boobies: %s\n", str);
                        fileEntityFuture.complete(FileEntity.builder()
                                .name(str)
                                .size(filePropsFuture.result().size())
                                .rndsuffix("ddd")
                                .build()
                        );
                    } else {
                        System.out.println("adding cause to the cause");
                        fileEntityFuture.fail(filePropsFuture.cause());
                    }
                }
        );

        return fileEntityFuture;
    }

}
