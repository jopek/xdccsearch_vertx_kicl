package com.lxbluem.filesystem;

import com.lxbluem.filesystem.repository.FilenameRepository;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;

import java.util.ArrayList;
import java.util.List;

public class FilenameResolverVerticle extends AbstractVerticle {
    private static final String PATH = "downloads";


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", FilenameResolverVerticle.class.getName()});
    }

    @Override
    public void start() {
//        vertx.eventBus().consumer("filename.resolve", handler -> {
//            JsonObject body = (JsonObject) handler.body();
//            String requestedFilename = body.getString("filename");
//        });
//        repository = new FilenameRepositoryImpl(vertx, "downloads.db");

        cleanDb();
    }

    private void cleanDb() {
//        Future<List<FileEntity>> allDbEntries = getAllDbEntries();

    }

//    private Future<List<FileEntity>> getAllDbEntries() {
//        Future<List<FileEntity>> allDbEntries = Future.future();
//        repository.retrieveAll()
//                .reduce(new ArrayList<FileEntity>(), (list, entity) -> {
//                    list.add(entity);
//                    return list;
//                })
//                .subscribe(allDbEntries::complete, allDbEntries::fail);
//        return allDbEntries;
//    }

}
