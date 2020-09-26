package com.lxbluem.search.adapters.ixirc;

import com.lxbluem.search.domain.ports.SearchGateway;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class IxircSearchGatewayResponseMapperTest {

    private final JsonObject INPUT = new JsonObject()
            .put("c", 61)
            .put("pc", 3)
            .put("pn", 1)
            .put("t", 1599649535)
            .put("results", new JsonArray()
                    .add(
                            new JsonObject()
                                    .put("pid", 1153762325)
                                    .put("name", "Terminator.1..1984.Remastered.MULTi.1080p.Bluray.HDLight.AC3.x264..schwarzenegger.mkv")
                                    .put("nid", 4)
                                    .put("nname", "Rizon")
                                    .put("naddr", "irc.rizon.net")
                                    .put("nport", 6667)
                                    .put("cid", 2013768364)
                                    .put("cname", "#ELITEWAREZ")
                                    .put("uid", 1685630753)
                                    .put("uname", "[EWG]SeedS0x13")
                                    .put("n", 4879)
                                    .put("gets", 1)
                                    .put("sz", 2899102924L)
                                    .put("szf", "2.7 GB")
                                    .put("age", 1598544436)
                                    .put("agef", "12 day ago")
                                    .put("last", 1599600559)
                                    .put("lastf", "13 hr ago")
                    ));

    @Test
    public void map() {
        SearchGateway.SearchResponse mapped = IxircResponseMapper.mapToSearchResponse(INPUT);

        SearchGateway.SearchResponse expected = SearchGateway.SearchResponse.builder()
                .hasMore(true)
                .currentPage(1)
                .results(Collections.singletonList(SearchGateway.ResponsePack.builder()
                        .packName("Terminator.1..1984.Remastered.MULTi.1080p.Bluray.HDLight.AC3.x264..schwarzenegger.mkv")
                        .networkName("Rizon")
                        .serverHostName("irc.rizon.net")
                        .serverPort(6667)
                        .channelName("#ELITEWAREZ")
                        .nickName("[EWG]SeedS0x13")
                        .packNumber(4879)
                        .packGets(1)
                        .sizeBytes(2899102924L)
                        .age(1598544436)
                        .last(1599600559)
                        .build()))
                .build();

        assertEquals(expected, mapped);
    }

    @Test
    public void map_no_more_results() {
        JsonObject modifiedCopy = INPUT.copy().put("pn", 2);

        SearchGateway.SearchResponse mapped = IxircResponseMapper.mapToSearchResponse(modifiedCopy);

        SearchGateway.SearchResponse expected = SearchGateway.SearchResponse.builder()
                .hasMore(false)
                .currentPage(2)
                .results(Collections.singletonList(SearchGateway.ResponsePack.builder()
                        .packName("Terminator.1..1984.Remastered.MULTi.1080p.Bluray.HDLight.AC3.x264..schwarzenegger.mkv")
                        .networkName("Rizon")
                        .serverHostName("irc.rizon.net")
                        .serverPort(6667)
                        .channelName("#ELITEWAREZ")
                        .nickName("[EWG]SeedS0x13")
                        .packNumber(4879)
                        .packGets(1)
                        .sizeBytes(2899102924L)
                        .age(1598544436)
                        .last(1599600559)
                        .build()))
                .build();

        assertEquals(expected, mapped);
    }
}