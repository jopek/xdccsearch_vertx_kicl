package com.lxbluem.filesystem;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

public class FileNameRepository {
    private final Vertx vertx;
    private JDBCClient jdbcClient;

    public FileNameRepository(Vertx vertx) {
        this.vertx = vertx;
        jdbcClient = JDBCClient.createNonShared(vertx, new JsonObject()
                .put("url", "jdbc:sqlite:filenames.sqlite")
//                .put("driver_class", "org.xerial.JDBC")
        );

        jdbcClient.getConnection(conn -> {
            if (conn.failed())
                throw new RuntimeException(conn.cause());

            SQLConnection connection = conn.result();
            System.out.println("connection: " + connection);
        });
    }
}
