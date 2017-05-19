package search;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.commons.lang3.StringUtils;

public class Search extends AbstractVerticle {

    public static final String RESULTS = "search.results";

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.route("/search").handler(this::handleSearchRequest);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080);
    }

    private void handleSearchRequest(RoutingContext routingContext) {
        MultiMap params = routingContext.request().params();

        String pageNum = params.get("pn");
        if (StringUtils.isEmpty(pageNum) || pageNum.equalsIgnoreCase("undefined")) {
            pageNum = "0";
        }

        String query = params.get("q");
        System.out.printf("query:%s  pagenum:%s\n", query, pageNum);
        doSearch(query, pageNum, routingContext.response());
    }

    private void doSearch(String query, String pageNum, HttpServerResponse routingResponse) {
        WebClient client = WebClient.create(vertx);

        client.get("ixirc.com", "/api")
                .addQueryParam("q", query)
                .addQueryParam("pn", pageNum)
                .as(BodyCodec.jsonObject())
                .send(event -> handleSearchResponse(event, routingResponse));
    }

    private void handleSearchResponse(AsyncResult<HttpResponse<JsonObject>> ar, HttpServerResponse routingResponse) {
        if (ar.succeeded()) {
            HttpResponse<JsonObject> searchResponse = ar.result();
            System.out.println("Got HTTP searchResponse body");
            routingResponse.end(searchResponse.body().encodePrettily());
        } else {
            ar.cause().printStackTrace();
            routingResponse.end();
        }
    }
}
