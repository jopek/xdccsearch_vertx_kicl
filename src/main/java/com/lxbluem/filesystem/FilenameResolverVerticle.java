package com.lxbluem.filesystem;

import io.vertx.core.Launcher;
import io.vertx.rxjava.core.AbstractVerticle;

public class FilenameResolverVerticle extends AbstractVerticle {
    private FileNameRepository repository;


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", FilenameResolverVerticle.class.getName()});
    }

    @Override
    public void start() {
//        vertx.eventBus().consumer("filename.resolve", handler -> {
//            JsonObject body = (JsonObject) handler.body();
//            String requestedFilename = body.getString("filename");
//        });
        repository = new FileNameRepository(vertx);

    }
}
