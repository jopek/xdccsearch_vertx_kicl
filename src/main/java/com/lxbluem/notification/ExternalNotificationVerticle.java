package com.lxbluem.notification;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lxbluem.common.infrastructure.Address.DCC_FINISHED;
import static java.lang.String.format;

public class ExternalNotificationVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalNotificationVerticle.class);
    private JsonObject clientConfig;
    private WebClient client;


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", ExternalNotificationVerticle.class.getName()});
    }

    @Override
    public void start() {
        client = WebClient.create(vertx);

        readConfiguration();

        vertx.eventBus().consumer(DCC_FINISHED.address(), handler -> {
            JsonObject body = (JsonObject) handler.body();
            JsonObject pack = body.getJsonObject("pack");

            JsonObject requestData = new JsonObject()
                    .put("value1", pack.getString("szf"))
                    .put("value2", pack.getString("name"));

            sendNotification(requestData);
        });

    }

    private void readConfiguration() {
        ConfigStoreOptions store = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("path", "webhooks.yaml")
                        .put("cache", "false")
                );

        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions().addStore(store);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, retrieverOptions);

        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                JsonObject config = ar.result();
                String string = config.getString("use");
                clientConfig = config.getJsonObject(string);
                LOG.debug("using config {}", clientConfig.encode());
            }
        });
    }

    private void sendNotification(JsonObject requestPayload) {
        String message = format("%s://%s:%d%s",
                Boolean.TRUE.equals(clientConfig.getBoolean("useSsl")) ? "https" : "http",
                clientConfig.getString("host"),
                clientConfig.getInteger("port"),
                clientConfig.getString("uri")
        );
        LOG.debug("send request to {}", message);

        client
                .post(
                        clientConfig.getInteger("port"),
                        clientConfig.getString("host"),
                        clientConfig.getString("uri")
                )
                .ssl(clientConfig.getBoolean("useSsl", false))
                .sendJsonObject(requestPayload, clientResponse -> {
                    if (clientResponse.succeeded()) {
                        LOG.info("successful post: {}", clientResponse.result().body());
                    } else {
                        LOG.warn("failure to post", clientResponse.cause());
                    }
                });
    }
}
