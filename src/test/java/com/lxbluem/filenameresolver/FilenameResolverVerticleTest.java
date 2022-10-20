package com.lxbluem.filenameresolver;


import com.lxbluem.filenameresolver.adapters.InMemoryEntityStorage;
import com.lxbluem.filenameresolver.domain.interactors.FilenameMapper;
import com.lxbluem.filenameresolver.domain.interactors.ResolvePackNameImpl;
import com.lxbluem.filenameresolver.domain.interactors.SyncStorageFromFsImpl;
import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.incoming.SyncStorageFromFs;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class FilenameResolverVerticleTest {
    private FileSystemBlocking fileSystem;
    private InMemoryEntityStorage storage;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {

        FilenameMapper filenameMapper = new FilenameMapper();
        storage = new InMemoryEntityStorage();
        fileSystem = mock(FileSystemBlocking.class);
        String downloadsPath = "downloads";
        ResolvePackNameImpl resolvePackName = new ResolvePackNameImpl(filenameMapper, fileSystem, storage, downloadsPath);

        SyncStorageFromFs syncStorageFromFs = new SyncStorageFromFsImpl(fileSystem, storage, downloadsPath);
        Verticle verticle = new FilenameResolverVerticle(resolvePackName, syncStorageFromFs);

        Checkpoint async = testContext.checkpoint();
        vertx.deployVerticle(verticle, testContext.succeeding(event -> async.flag()));

    }

    @AfterEach
    void tearDown(Vertx vertx, VertxTestContext testContext) {
        vertx.close(testContext.succeeding(p->testContext.completeNow()));
    }

    @Test
    void synchronise_storage_on_verticle_start(VertxTestContext testContext) {
        when(fileSystem.readDir("downloads")).thenReturn(Arrays.asList(
                "/canonical/path/downloads/file._x0x_.bin.part",
                "/canonical/path/downloads/file._x1x_.bin.part",
                "/canonical/path/downloads/file2._x0x_.bin",
                "/canonical/path/downloads/file2._x1x_.bin.part",
                "/canonical/path/downloads/file2._x2x_.bin"
        ));
        storage.save(new FileEntity("file.bin", 1000L, "file._x0x_.bin.part", true));
        storage.save(new FileEntity("file.bin", 2000L, "file._x1x_.bin.part", true));
        storage.save(new FileEntity("file2.bin", 9000L, "file._x1x_.bin.part", false));

        testContext.verify(() -> {
            List<FileEntity> fe1 = storage.findByNameAndSize("file.bin", 1000L);
            assertEquals(1, fe1.size());
            assertTrue(fe1.get(0).isInUse());
            List<FileEntity> fe2 = storage.findByNameAndSize("file.bin", 2000L);
            assertEquals(1, fe2.size());
            assertTrue(fe2.get(0).isInUse());
            List<FileEntity> fe3 = storage.findByNameAndSize("file2.bin", 9000L);
            assertEquals(1, fe3.size());
            assertFalse(fe3.get(0).isInUse());
        });
        testContext.completeNow();
    }

    @Test
    void name(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        JsonObject message = new JsonObject()
                .put("packname", "file1a")
                .put("packsize", 2000L);

        vertx.eventBus().request(FILENAME_RESOLVE.address(), message, testContext.succeeding(reply -> {
            testContext.verify(() -> assertThat((JsonObject) reply.body())
                    .isEqualTo(new JsonObject()
                            .put("filename", "downloads/file1a._x0x_.part")
                            .put("position", 0)
                            .put("isComplete", false)
                    ));

            List<FileEntity> fileEntities = storage.findByName("file1a");
            testContext.verify(
                    () -> assertThat(fileEntities)
                            .extracting(FileEntity::isInUse, FileEntity::getPacksize)
                            .containsExactly(Tuple.tuple(true, 2000L))

            );
            testContext.completeNow();
        }));
        testContext.awaitCompletion(1, TimeUnit.SECONDS);


    }
}
