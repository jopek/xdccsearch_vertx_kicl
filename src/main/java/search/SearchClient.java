package search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxbluem.irc.pack.PackInfo;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

public class SearchClient {
    private static Logger logger = LoggerFactory.getLogger(SearchClient.class);

    private ClientSettings settings;

    private OkHttpClient client;

    private ObjectMapper objectMapper;

    @Inject
    public SearchClient(ClientSettings settings, ObjectMapper objectMapper) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient();
    }

    public PagedResult<PackInfo> search(String queryString, int page) throws IOException {


        HttpClient httpClient = Vertx.vertx().createHttpClient();

        HttpUrl.Builder builder = getBuilderTemplate();

        if (settings.getQueryParam() != null) {
            builder.addQueryParameter(settings.getQueryParam(), queryString);
        }

        if (settings.getPageParam() != null) {
            builder.addQueryParameter(settings.getPageParam(), String.valueOf(page));
        }

        if (settings.getPath() != null) {
            builder.addPathSegment(settings.getPath());
        }

        if (settings.isUsePathForQuery()) {
            builder.addPathSegment(queryString);
        }

        HttpUrl httpUrl = builder.build();

        Request request = new Request.Builder().url(httpUrl).get().build();

        logger.debug("requesting {}", request.toString());

        Response response = client.newCall(request).execute();
        int httpStatusCode = response.code();
        if (httpStatusCode >= 400) {
            logger.warn("status {}", httpStatusCode);
        }
        String content = response.body().string();
        return objectMapper.readValue(content, new TypeReference<PagedResult<PackInfo>>() {});
    }

    private HttpUrl.Builder getBuilderTemplate() {
        return new HttpUrl.Builder().scheme(settings.getScheme()).host(settings.getHost()).port(settings.getPort());
    }
}
