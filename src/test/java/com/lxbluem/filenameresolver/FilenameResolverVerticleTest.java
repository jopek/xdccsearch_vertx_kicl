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
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class FilenameResolverVerticleTest {
    private Vertx vertx;
    private Verticle verticle;
    private FileSystemBlocking fileSystem;
    private InMemoryEntityStorage storage;

    @Before
    public void setUp(TestContext testContext) {
        vertx = Vertx.vertx();

        FilenameMapper filenameMapper = new FilenameMapper();
        storage = new InMemoryEntityStorage();
        fileSystem = mock(FileSystemBlocking.class);
        String downloadsPath = "downloads";
        ResolvePackNameImpl resolvePackName = new ResolvePackNameImpl(filenameMapper, fileSystem, storage, downloadsPath);

        SyncStorageFromFs syncStorageFromFs = new SyncStorageFromFsImpl(fileSystem, storage, downloadsPath);
        verticle = new FilenameResolverVerticle(resolvePackName, syncStorageFromFs);
    }

    @After
    public void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }

    @Test
    public void synchronise_storage_on_verticle_start(TestContext testContext) {
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

        Async async = testContext.async();
        vertx.deployVerticle(verticle, testContext.asyncAssertSuccess(event -> async.complete()));
        async.await();

        testContext.verify(event -> {
            List<FileEntity> fe1 = storage.findByNameAndSize("file.bin", 1000L);
            assertEquals(1, fe1.size());
            assertFalse(fe1.get(0).isInUse());
            List<FileEntity> fe2 = storage.findByNameAndSize("file.bin", 2000L);
            assertEquals(1, fe2.size());
            assertFalse(fe2.get(0).isInUse());
            List<FileEntity> fe3 = storage.findByNameAndSize("file2.bin", 9000L);
            assertEquals(1, fe3.size());
            assertFalse(fe3.get(0).isInUse());
        });
    }

    @Test
    public void name(TestContext testContext) {
        JsonObject message = new JsonObject()
                .put("packname", "file1a")
                .put("packsize", 2000L);

        Async async = testContext.async();
        vertx.deployVerticle(verticle, testContext.asyncAssertSuccess(event -> async.complete()));
        async.await();

        vertx.eventBus().request(FILENAME_RESOLVE.address(), message, testContext.asyncAssertSuccess(reply -> {
            JsonObject body = (JsonObject) reply.body();
            testContext.assertEquals("downloads/file1a._x0x_.part", body.getString("filename"));

            List<FileEntity> fileEntities = storage.findByNameAndSize("file1a", 2000L);
            testContext.assertEquals(1, fileEntities.size());
            testContext.assertTrue(fileEntities.get(0).isInUse());
        }));


    }
}