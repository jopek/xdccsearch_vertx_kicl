package com.lxbluem.filesystem;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FilenameResolverVerticleTest {
    private Vertx vertx;

    @Before
    public void setUp() throws Exception {
        vertx = Vertx.vertx();

        AtomicBoolean completed = new AtomicBoolean();
        vertx.deployVerticle(new FilenameResolverVerticle(), ar -> completed.set(ar.succeeded()));
        await().untilAtomic(completed, is(true));
    }


    @After
    public void tearDown() throws Exception {
        AtomicBoolean completed = new AtomicBoolean();
        vertx.close((v) -> completed.set(true));
        await().untilAtomic(completed, is(true));
    }

    @Test
    public void name() throws Exception {
        JsonObject message = new JsonObject().put("filename", "file1a");
        vertx.eventBus().send(FilenameResolverVerticle.address, message, replyHandler -> {
            assertTrue(replyHandler.succeeded());
            JsonObject body = (JsonObject) replyHandler.result().body();
            assertEquals(FilenameResolverVerticle.PATH + "/" + "file1a.part", body.getString("filename"));
        });
    }
}