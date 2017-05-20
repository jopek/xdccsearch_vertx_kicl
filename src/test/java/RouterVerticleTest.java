import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.junit.Before;
import org.junit.Test;

import static io.vertx.core.http.HttpMethod.GET;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouterVerticleTest {

    private RouterVerticle routerVerticle;

    @Before
    public void setUp() throws Exception {
        routerVerticle = new RouterVerticle();
    }

    @Test
    public void testSetupRouter() {
        Router router = mock(Router.class);
        Message<Object> message = mock(Message.class);

        JsonObject messageObject = new JsonObject()
                .put("method", GET)
                .put("path", "/vertiRoute");
        when(message.body()).thenReturn(messageObject);
        routerVerticle.setupRouter(router, message);

        verify(router).route(GET, "/vertiRoute");
    }

}