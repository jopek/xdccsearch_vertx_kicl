package com.lxbluem.search.adapters.sunxdcc;

import com.lxbluem.search.domain.callback.Callback;
import com.lxbluem.search.domain.ports.SearchGateway;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class SunXDccSearchGateway implements SearchGateway {
    private final WebClient client;
    private final Configuration config;

    public SunXDccSearchGateway(io.vertx.core.Vertx vertx) {
        Vertx rxVertxInstance = Vertx.newInstance(vertx);
        WebClientOptions clientOptions = new WebClientOptions()
                .setFollowRedirects(true);
        this.client = WebClient.create(rxVertxInstance, clientOptions);
        this.config = new Configuration();
    }

    public SunXDccSearchGateway(io.vertx.core.Vertx vertx, Configuration config) {
        Vertx rxVertxInstance = Vertx.newInstance(vertx);
        WebClientOptions clientOptions = new WebClientOptions()
                .setFollowRedirects(true);
        this.client = WebClient.create(rxVertxInstance, clientOptions);
        this.config = config;
    }

    @Override
    public void doSearch(SearchQuery query, Callback<SearchResponse> consumer) {
        int pageNumber = query.getPageNumber();
        String searchTerm = query.getSearchTerm();

        int port = config.getPort();
        String host = config.getHost();
        String requestURI = config.getRequestURI();
        client.get(port, host, requestURI)
                .addQueryParam("sterm", searchTerm)
                .addQueryParam("page", String.valueOf(pageNumber))
                .as(BodyCodec.jsonObject())
                .rxSend()
                .map(HttpResponse::body)
                .map(jsonObject -> jsonObject.mapTo(SunXDccResponse.class))
                .map(sunXDccResponse -> SunXDccResponseMapper.mapToSearchResponse(sunXDccResponse, pageNumber))
                .subscribe(consumer::success, consumer::failure);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Configuration {
        private int port = 80;
        private String host = "sunxdcc.com";
        private String requestURI = "/deliver.php";
    }
}
