package com.lxbluem.notification;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

public class ExternalNotificationVerticle extends AbstractVerticle {
    private String url;
    private HttpClient client;


    public static void main(String[] args) {
        Launcher.main(new String[]{"run", ExternalNotificationVerticle.class.getName()});
    }

    @Override
    public void start() {
        client = vertx.createHttpClient();

        ConfigStoreOptions store = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("path", "webhooks.yaml")
                );

        ConfigRetriever retriever = ConfigRetriever.create(vertx,
                new ConfigRetrieverOptions().addStore(store));

        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                JsonObject config = ar.result();
                url = config.getString("url");
                System.out.println(url);
            }
        });

        vertx.eventBus().consumer("bot.dcc.finish", handler -> {
            JsonObject body = (JsonObject) handler.body();
            JsonObject pack = body.getJsonObject("pack");

            JsonObject requestData = new JsonObject()
                    .put("value1", pack.getString("szf"))
                    .put("value2", pack.getString("name"));

            client.post(url, response -> {
                System.out.println("Received response with status code " + response.statusCode());
            })
                    .putHeader("content-type", "application/json")
                    .write(requestData.encode())
                    .handler(clientResponse -> {
                        clientResponse.bodyHandler(event -> System.out.println(event.toString()));
                    })
                    .end();
        });
    }
}
