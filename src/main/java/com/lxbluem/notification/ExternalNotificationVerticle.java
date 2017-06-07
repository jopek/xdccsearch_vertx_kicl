package com.lxbluem.notification;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import static java.lang.String.format;

public class ExternalNotificationVerticle extends AbstractVerticle {
    private JsonObject clientConfig;
    private WebClient client;


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", ExternalNotificationVerticle.class.getName()});
    }

    @Override
    public void start() {
        client = WebClient.create(vertx);

        updateConfiguration();

        vertx.eventBus().consumer("bot.dcc.finish", handler -> {
            JsonObject body = (JsonObject) handler.body();
            JsonObject pack = body.getJsonObject("pack");

            JsonObject requestData = new JsonObject()
                    .put("value1", pack.getString("szf"))
                    .put("value2", pack.getString("name"));

        });

    }

    private void updateConfiguration() {
        ConfigStoreOptions store = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("path", "webhooks.yaml")
                        .put("cache", "false")
                );

        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions()
                .setScanPeriod(1000)
                .addStore(store);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, retrieverOptions);

        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                updateConfig(ar.result());
            }
        });

        retriever.listen(change -> {
            System.out.println("CHANGE");
            updateConfig(change.getNewConfiguration());
        });
    }

    private void updateConfig(JsonObject config) {
        String string = config.getString("use");
        clientConfig = config.getJsonObject(string);

        System.out.println(clientConfig.encode());

//        xxx();
    }

    private void xxx() {
        JsonObject requestPayload = new JsonObject().put("value1", "bllllaaaaaaa");

        System.out.println("send request to " + format("%s://%s:%d%s",
                clientConfig.getBoolean("useSsl") ? "https" : "http",
                clientConfig.getString("host"),
                clientConfig.getInteger("port"),
                clientConfig.getString("uri")
        ));

        client
                .post(
                        clientConfig.getInteger("port"),
                        clientConfig.getString("host"),
                        clientConfig.getString("uri"))
                .ssl(clientConfig.getBoolean("useSsl", false))
                .sendJsonObject(requestPayload, clientResponse -> {
                    if (clientResponse.succeeded()) {
                        System.out.println("RESULT: " + clientResponse.result().body());
                    } else {
                        System.out.println("failure cause: " + clientResponse.cause().getMessage());
                    }
                });
    }
}
