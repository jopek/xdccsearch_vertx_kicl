package com.lxbluem.search;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.vertx.core.http.HttpMethod.GET;

public class SearchVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(SearchVerticle.class);
    private static final String START_SEARCH = "onStartSearch";

    @Override
    public void start() {
        String searchString = config().getString(START_SEARCH, "");
        if (!searchString.isEmpty()) {
            Future<JsonObject> future = Future.future();
            future.setHandler(ar -> {
                if(ar.succeeded()) {
                    LOG.info(ar.result().encodePrettily());
                }
            });
            doSearch(searchString, "0", future);
        } else {
            registerRouteWithHandler(GET, "/search", this::handleSearchRequest);
        }
    }


    private void handleSearchRequest(SerializedRequest request, Future<JsonObject> responseHandler) {
        Map<String, String> params = request.getParams();

        String pageNum = params.get("pn");
        if (StringUtils.isEmpty(pageNum) || pageNum.equalsIgnoreCase("undefined")) {
            pageNum = "0";
        }

        String query = params.get("q");
        if (StringUtils.isEmpty(query)) {
            responseHandler.fail("query is empty");
            return;
        }

        doSearch(query, pageNum, responseHandler);
    }

    private void doSearch(String query, String pageNum, Future<JsonObject> responseHandler) {
        WebClientOptions clientOptions = new WebClientOptions()
                .setFollowRedirects(true);
        WebClient client = WebClient.create(vertx, clientOptions);

        LOG.info("search for {}, page {}", query, pageNum);

        client.get("ixirc.com", "/api/")
                .addQueryParam("q", query)
                .addQueryParam("pn", pageNum)
                .as(BodyCodec.jsonObject())
                .rxSend()
                .subscribe(
                        httpResponse -> responseHandler.complete(httpResponse.body()),
                        throwable -> {
                            LOG.error("could not complete request: {}", throwable.getMessage());
                            responseHandler.fail(throwable);
                        }
                );
    }

    public static void main(String[] args) {
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(
                        new JsonObject()
                                .put(START_SEARCH, "muh")
                );
        Vertx.vertx().deployVerticle(SearchVerticle.class.getName(), options);
    }
}
