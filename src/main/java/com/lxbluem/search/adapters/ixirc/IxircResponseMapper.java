package com.lxbluem.search.adapters.ixirc;

import com.lxbluem.search.domain.ports.SearchGateway;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class IxircResponseMapper {
    private IxircResponseMapper(){}

    public static SearchGateway.SearchResponse mapToSearchResponse(JsonObject jsonObject) {
        JsonArray ixircResults = jsonObject.getJsonArray("results", new JsonArray());
        int pageCount = jsonObject.getInteger("pc", 0);
        int currentPage = jsonObject.getInteger("pn", 0);
        boolean hasMore = pageCount - (currentPage + 1) > 0 && !ixircResults.isEmpty();

        List<SearchGateway.ResponsePack> results = ixircResults.stream()
                .map(result -> {
                    IxircResponse o = ((JsonObject) result).mapTo(IxircResponse.class);
                    return SearchGateway.ResponsePack.builder()
                            .channelName(o.getCname())
                            .networkName(o.getNname())
                            .serverHostName(o.getNaddr())
                            .serverPort(o.getNport())
                            .nickName(o.getUname())
                            .packGets(o.getGets())
                            .packName(o.getName())
                            .packNumber(o.getN())
                            .sizeBytes(o.getSz())
                            .age(o.getAge())
                            .last(o.getLast())
                            .build();

                })
                .collect(Collectors.toList());

        return SearchGateway.SearchResponse.builder()
                .hasMore(hasMore)
                .currentPage(currentPage)
                .results(results)
                .build();
    }

}
