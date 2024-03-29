package com.lxbluem.filenameresolver;

import com.lxbluem.filenameresolver.domain.ports.incoming.ResolvePackName;
import com.lxbluem.filenameresolver.domain.ports.incoming.SyncStorageFromFs;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import rx.Completable;

import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;

public class FilenameResolverVerticle extends AbstractVerticle {
    private final ResolvePackName resolver;
    private final SyncStorageFromFs syncStorageFromFs;

    public FilenameResolverVerticle(ResolvePackName resolver, SyncStorageFromFs syncStorageFromFs) {
        this.resolver = resolver;
        this.syncStorageFromFs = syncStorageFromFs;
    }

    @Override
    public Completable rxStart() {
        syncStorageFromFs.execute();
        vertx.eventBus().<JsonObject>consumer(FILENAME_RESOLVE.address())
                .toObservable()
                .subscribe(message -> {
                    JsonObject body = message.body();
                    String requestedFilename = body.getString("packname");
                    long requestedFileSize = body.getLong("packsize");

                    resolver.execute(requestedFilename, requestedFileSize)
                            .subscribe(response -> {
                                JsonObject jsonResponse = new JsonObject()
                                        .put("filename", response.getFilename())
                                        .put("position", response.getFilesize())
                                        .put("isComplete", response.isComplete());
                                message.reply(jsonResponse);
                            });
                });

        return Completable.complete();
    }
}
