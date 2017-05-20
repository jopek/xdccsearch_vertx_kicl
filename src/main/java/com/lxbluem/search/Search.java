package com.lxbluem.search;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static io.vertx.core.http.HttpMethod.GET;

public class Search extends AbstractRouteVerticle {

  @Override
  public void start() {
    registerRoute(GET, "/search");

    vertx.eventBus().consumer(getClass().getSimpleName(), message -> {
      String messageBody = message.body().toString();
      SerializedRequest request = Json.decodeValue(messageBody, SerializedRequest.class);

      Future<JsonObject> future = Future.future();
      future.compose(message::reply, Future.future().setHandler(e -> message.fail(500, e.cause().getMessage())));

      handleSearchRequest(request, future);
    });
  }


  private void handleSearchRequest(SerializedRequest request, Future<JsonObject> responseHandler) {
    Map<String, String> params = request.getParams();

    String pageNum = params.get("pn");
    if (StringUtils.isEmpty(pageNum) || pageNum.equalsIgnoreCase("undefined")) {
      pageNum = "0";
    }

    String query = params.get("q");
    if (StringUtils.isEmpty(query))
      responseHandler.fail("query is empty");

    doSearch(query, pageNum, responseHandler);
  }

  private void doSearch(String query, String pageNum, Future<JsonObject> responseHandler) {
    WebClient client = WebClient.create(vertx);

    client.get("ixirc.com", "/api")
        .addQueryParam("q", query)
        .addQueryParam("pn", pageNum)
        .as(BodyCodec.jsonObject())
        .send(event -> handleSearchResponse(event, responseHandler));
  }

  private void handleSearchResponse(AsyncResult<HttpResponse<JsonObject>> ar, Future<JsonObject> routingResponse) {
    if (ar.succeeded()) {
      HttpResponse<JsonObject> searchResponse = ar.result();
      routingResponse.complete(searchResponse.body());
    } else {
      routingResponse.fail(ar.cause());
    }
  }
}
