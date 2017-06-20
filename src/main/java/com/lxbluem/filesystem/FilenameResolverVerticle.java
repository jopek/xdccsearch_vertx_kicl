package com.lxbluem.filesystem;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;

public class FilenameResolverVerticle extends AbstractVerticle {
    private static final String PATH = "downloads";

    public static final String address = "filename.resolve";


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", FilenameResolverVerticle.class.getName()});
    }

    @Override
    public void start() {
        FilenameResolver resolver = new FilenameResolver(vertx, PATH, new FilenameMapper());

        vertx.eventBus().consumer(address, handler -> {
            JsonObject body = (JsonObject) handler.body();
            String requestedFilename = body.getString("filename");
            System.out.println(getClass().getSimpleName() + " asked for " + requestedFilename);

            resolver.getFileNameForPackName(requestedFilename).setHandler(filename -> {
                        System.out.println(getClass().getSimpleName() + " replying with " + filename.result());
                        if (filename.succeeded())
                            handler.reply(new JsonObject().put("filename", filename.result()));
                    }
            );
        });

    }


}
