package com.lxbluem.search;

import com.lxbluem.search.adapters.ixirc.IxircSearchGateway;
import com.lxbluem.search.domain.interactors.ListMatchingPacksImpl;
import com.lxbluem.search.domain.ports.ListMatchingPacks;
import com.lxbluem.search.domain.ports.SearchGateway;
import io.vertx.core.MultiMap;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class SearchVerticleTest {
    private final String searchAddress = "SearchVerticle:GET:/search";

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext context) {
        IxircSearchGateway.Configuration config = new IxircSearchGateway.Configuration(12321, "localhost", "");
        SearchGateway searchGateway = new IxircSearchGateway(vertx, config);
//        SearchGateway searchGateway = new IxircSearchGateway(vertx);
        ListMatchingPacks listMatchingPacks = new ListMatchingPacksImpl(searchGateway);
        Verticle verticle = new SearchVerticle(listMatchingPacks);

        Checkpoint checkpoint = context.checkpoint();
        vertx.deployVerticle(verticle, x -> {
            System.out.println("deployed" + x.result());
            checkpoint.flag();
        });
        ;
    }

    @Test
    @Timeout(value = 3)
    void verticle_does_http_request(Vertx vertx, VertxTestContext context) throws InterruptedException {
        Checkpoint request = context.checkpoint();
        vertx.createHttpServer()
                .requestHandler(r -> {
                    request.flag();
                    MultiMap params = r.params();
                    assertThat(params.get("q")).isEqualTo("poo");
                    assertThat(params.get("pn")).isEqualTo("1");
                    assertEquals(2, params.size());
                    r.response().end(new JsonObject().encode());
                })
                .listen(12321);

        final JsonObject searchMessage = new JsonObject()
                .put("method", "GET")
                .put("params", new JsonObject()
                        .put("q", "poo")
                        .put("pn", "1")
                );

        Checkpoint replied = context.checkpoint();
        vertx.eventBus()
                .<JsonObject>request(searchAddress, searchMessage, context.succeeding(m -> {
                    JsonObject body = m.body();
                    assertThat(body).isEqualTo(new JsonObject()
                            .put("results", new JsonArray())
                            .put("pn", 0)
                            .put("pc", 0));
                    replied.flag();
                }));

        context.awaitCompletion(100, TimeUnit.MILLISECONDS);
    }

    @Test
    @Timeout(value = 30)
    void verticle_does_returns_error_when_no_searchterm_provided(Vertx vertx, VertxTestContext context) throws InterruptedException {
        final JsonObject searchMessage = new JsonObject()
                .put("method", "GET")
                .put("params", new JsonObject());


        vertx.eventBus()
                .<JsonObject>request(
                        searchAddress,
                        searchMessage,
                        context.failing(t -> {
                            assertThat(t).hasMessage("please provide a searchTerm for jsonobject key q");
                            context.completeNow();
                        })
                );

        context.awaitCompletion(100, TimeUnit.MILLISECONDS);
//        m ->
//                context.verify(unused -> {
//                    assertEquals("please provide a searchTerm for jsonobject key q", m.getMessage());
//                })
    }

}
