package search;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxbluem.ControllerBase;
import com.lxbluem.ErrorResponse;
import spark.utils.StringUtils;

import javax.inject.Inject;
import java.io.IOException;

import static spark.Spark.*;

public class SearchController extends ControllerBase {
    private SearchService searchService;
    private ObjectMapper objectMapper;

    @Inject
    public SearchController(SearchService searchService, ObjectMapper objectMapper) {
        super("/search");
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }

    public void setup() {
        get(getRootRoute(), (req, res) -> {
            String pageNum = req.queryParams("pn");
            if (StringUtils.isEmpty(pageNum) || pageNum.equalsIgnoreCase("undefined")) {
                pageNum = "0";
            }
            return searchService.searchFor(req.queryParams("q"), Integer.parseInt(pageNum));
        }, objectMapper::writeValueAsString);

        exception(JsonParseException.class, (exception, request, response) -> {
            response.status(400);
            response.body(getExceptionJson(exception));
        });

        exception(JsonMappingException.class, (exception, request, response) -> {
            response.status(400);
            response.body(getExceptionJson(exception));
        });

        exception(IOException.class, (exception, request, response) -> {
            response.status(400);
            response.body(getExceptionJson(exception));
        });

        after((request, response) -> response.type("application/json"));
    }

    private String getExceptionJson(Exception exception) {
        exception.printStackTrace();
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.message = String.format("error: %s", exception.getClass().getName());

        try {
            return objectMapper.writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "error creating error response for " + exception.getMessage() + " - because of: " + e.getMessage();
        }
    }
}
