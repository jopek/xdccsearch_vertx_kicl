package com.lxbluem.filesystem;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lxbluem.Addresses.FILENAME_RESOLVE;

public class FilenameResolverVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(FilenameResolverVerticle.class);
    public static final String PATH = "downloads";


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", FilenameResolverVerticle.class.getName()});
    }

    @Override
    public void start() {
        FilenameResolver resolver = new FilenameResolver(vertx, PATH, new FilenameMapper());

        vertx.eventBus().consumer(FILENAME_RESOLVE, handler -> {
            JsonObject body = (JsonObject) handler.body();
            String requestedFilename = body.getString("filename");
            resolver.getFileNameForPackName(requestedFilename)
                    .setHandler(filename -> {
                                if (filename.succeeded()) {
                                    LOG.debug("resolved {} -> {}", requestedFilename, filename.result());
                                    handler.reply(new JsonObject().put("filename", filename.result()));
                                }
                            }
                    );
        });

    }


}
