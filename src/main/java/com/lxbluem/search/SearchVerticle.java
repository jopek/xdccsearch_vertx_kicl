package com.lxbluem.search;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.infrastructure.AbstractRouteVerticle;
import com.lxbluem.common.infrastructure.SerializedRequest;
import com.lxbluem.search.domain.callback.Callback;
import com.lxbluem.search.domain.ports.ListMatchingPacks;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static io.vertx.core.http.HttpMethod.GET;

/*
 for compatibility reasons, the search verticle has multiple response mappings:
 - ixirc
   @com.lxbluem.search.adapters.ixirc.IxircSearchGateway:
   -> @JsonObject
   -> @com.lxbluem.search.adapters.ixirc.IxircResponse
   -> @com.lxbluem.search.domain.ports.SearchGateway.SearchResponse

   @ListMatchingPacks:
   -> @com.lxbluem.search.domain.ports.ListMatchingPacks.ListMatchingPacksResponse

   @Verticle:
   -> @com.lxbluem.common.domain.Pack
   -> @JsonObject

   TODO: shorten this mapping chain
 */


public class SearchVerticle extends AbstractRouteVerticle {
    private final ListMatchingPacks listMatchingPacks;

    public SearchVerticle(ListMatchingPacks listMatchingPacks) {
        this.listMatchingPacks = listMatchingPacks;
    }

    @Override
    public void start(Future<Void> startFuture) {
        registerRoute(GET, "/search", this::handleRoutedHttpSearchRequest)
                .onComplete(startFuture);
    }

    private void handleRoutedHttpSearchRequest(SerializedRequest request, Promise<JsonObject> responseHandler) {
        ListMatchingPacks.Command command = getSearchCommand(request);
        Callback<ListMatchingPacks.ListMatchingPacksResponse> presenter = searchResponsePresenter(responseHandler);
        listMatchingPacks.handle(command, presenter);
    }

    // temporarily map to ixirc Pack list response
    private Callback<ListMatchingPacks.ListMatchingPacksResponse> searchResponsePresenter(Promise<JsonObject> responseHandler) {
        return Callback.of(searchResponse -> {
            JsonArray packResults = new JsonArray();
            searchResponse.getResults().forEach(responsePack -> {
                Pack pack = Pack.builder()
                        .serverHostName(responsePack.getServerHostName())
                        .serverPort(responsePack.getServerPort())
                        .networkName(responsePack.getNetworkName())
                        .channelName(responsePack.getChannelName())
                        .packNumber(responsePack.getPackNumber())
                        .nickName(responsePack.getNickName())
                        .packName(responsePack.getPackName())
                        .packGets(responsePack.getPackGets())
                        .age(responsePack.getAge())
                        .sizeBytes(responsePack.getSizeBytes())
                        .lastAdvertised(responsePack.getLast())
                        .build();
                packResults.add(JsonObject.mapFrom(pack));
            });
            JsonObject response = new JsonObject()
                    .put("pn", searchResponse.getCurrentPage())
                    .put("pc", searchResponse.isHasMore()
                            ? searchResponse.getCurrentPage() + 2
                            : searchResponse.getCurrentPage())
                    .put("results", packResults);
            responseHandler.complete(response);
        }, responseHandler::fail);
    }

    private ListMatchingPacks.Command getSearchCommand(SerializedRequest request) {
        Map<String, String> params = request.getParams();

        String pageNumParam = params.getOrDefault("pn", "0");
        int pageNumber = 0;
        if (isValidPageNumber(pageNumParam))
            pageNumber = Integer.parseInt(pageNumParam);

        String searchTerm = params.getOrDefault("q", "");
        return new ListMatchingPacks.Command(searchTerm, pageNumber);
    }

    private boolean isValidPageNumber(String pageNumParam) {
        return !StringUtils.isEmpty(pageNumParam) && StringUtils.containsOnly(pageNumParam, "0123456789");
    }
}
