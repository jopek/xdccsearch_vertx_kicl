package com.lxbluem.filesystem;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.awaitility.Awaitility.await;
import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResolvePackNameVerticleTest {
    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        AtomicBoolean completed = new AtomicBoolean();
        vertx.deployVerticle(new FilenameResolverVerticle(), ar -> completed.set(ar.succeeded()));
        await().untilAtomic(completed, is(true));
    }

    @After
    public void tearDown() {
        AtomicBoolean completed = new AtomicBoolean();
        vertx.close((v) -> completed.set(true));
        await().untilAtomic(completed, is(true));
    }

    @Test
    public void name() {
        JsonObject message = new JsonObject().put("filename", "file1a");
        vertx.eventBus().send(FILENAME_RESOLVE.address(), message, replyHandler -> {
            assertTrue(replyHandler.succeeded());
            JsonObject body = (JsonObject) replyHandler.result().body();
            assertEquals(FilenameResolverVerticle.PATH + "/" + "file1a._0_.part", body.getString("filename"));
        });
    }
}