package com.lxbluem.search.adapters.ixirc;

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


public class IxircSearchGateway implements SearchGateway {
    private final Configuration config;
    private final WebClient client;

    public IxircSearchGateway(io.vertx.core.Vertx vertx, Configuration config) {
        Vertx rxVertxInstance = Vertx.newInstance(vertx);
        WebClientOptions clientOptions = new WebClientOptions()
                .setFollowRedirects(true);
        this.client = WebClient.create(rxVertxInstance, clientOptions);
        this.config = config;
    }

    public IxircSearchGateway(io.vertx.core.Vertx vertx) {
        Vertx rxVertxInstance = Vertx.newInstance(vertx);
        WebClientOptions clientOptions = new WebClientOptions()
                .setFollowRedirects(true);
        this.client = WebClient.create(rxVertxInstance, clientOptions);
        this.config = new IxircSearchGateway.Configuration();
    }

    @Override
    public void doSearch(SearchQuery query, Callback<SearchResponse> consumer) {
        int pageNumber = query.getPageNumber();
        String searchTerm = query.getSearchTerm();

        int port = config.getPort();
        String host = config.getHost();
        String requestURI = config.getRequestURI();

        client.get(port, host, requestURI)
                .addQueryParam("q", searchTerm)
                .addQueryParam("pn", String.valueOf(pageNumber))
                .as(BodyCodec.jsonObject())
                .rxSend()
                .map(HttpResponse::body)
                .map(IxircResponseMapper::mapToSearchResponse)
                .subscribe(consumer::success, consumer::failure);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Configuration {
        private int port = 80;
        private String host = "ixirc.com";
        private String requestURI = "/api";
    }

}

