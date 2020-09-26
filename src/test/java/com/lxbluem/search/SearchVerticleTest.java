package com.lxbluem.search;

import com.lxbluem.search.adapters.ixirc.IxircSearchGateway;
import com.lxbluem.search.domain.interactors.ListMatchingPacksImpl;
import com.lxbluem.search.domain.ports.ListMatchingPacks;
import com.lxbluem.search.domain.ports.SearchGateway;
import io.vertx.core.MultiMap;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class SearchVerticleTest {
    private Vertx vertx;

    private final String searchAddress = "SearchVerticle:GET:/search";

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();

        IxircSearchGateway.Configuration config = new IxircSearchGateway.Configuration(12321, "localhost", "");
//        SearchGateway searchGateway = new IxircSearchGateway(vertx, config);
        SearchGateway searchGateway = new IxircSearchGateway(vertx);
        ListMatchingPacks listMatchingPacks = new ListMatchingPacksImpl(searchGateway);
        Verticle verticle = new SearchVerticle(listMatchingPacks);
        Async async = context.async();
        vertx.deployVerticle(verticle, context.asyncAssertSuccess(v->async.complete()));
        async.await();
    }

    @Test(timeout = 30_000)
    public void verticle_does_http_request(TestContext context) {
        vertx.createHttpServer()
                .requestHandler(r -> {
                    MultiMap params = r.params();
                    context.verify(unused -> assertTrue(params.contains("q")));
                    context.verify(unused -> assertTrue(params.contains("pn")));
                    context.verify(unused -> assertEquals(2, params.size()));

                    System.out.println(params);
                    String response = new JsonObject().encode();
                    r.response().end(response);
                })
                .listen(12321);

        final JsonObject searchMessage = new JsonObject()
                .put("method", "GET")
                .put("params", new JsonObject()
                        .put("q", "poo")
                        .put("pn", "1")
                );


        vertx.eventBus()
                .<JsonObject>request(searchAddress, searchMessage, context.asyncAssertSuccess(m -> {
                    JsonObject body = m.body();
                    System.out.println(body.encodePrettily());
                    context.assertFalse(body.isEmpty());
                }));

    }

    @Test(timeout = 30_000)
    public void verticle_does_returns_error_when_no_searchterm_provided(TestContext context) {
        final JsonObject searchMessage = new JsonObject()
                .put("method", "GET")
                .put("params", new JsonObject());


        vertx.eventBus()
                .<JsonObject>request(searchAddress, searchMessage, context.asyncAssertFailure(m ->
                        context.verify(unused -> {
                            assertEquals("please provide a searchTerm for jsonobject key q", m.getMessage());
                        }))
                );

    }

}