package com.lxbluem.filesystem;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class FilenameResolverVerticle extends AbstractVerticle {
    private final FileNameRepository repository;

    public FilenameResolverVerticle(FileNameRepository repository) {
        this.repository = repository;
    }

    @Override
    public void start() {
        vertx.eventBus().consumer("filename.resolve", handler -> {
            JsonObject body = (JsonObject) handler.body();
            String requestedFilename = body.getString("filename");
        });
    }
}
