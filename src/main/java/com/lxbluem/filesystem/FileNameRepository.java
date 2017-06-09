package com.lxbluem.filesystem;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Single;

class FileNameRepository {
    private JDBCClient client;

    FileNameRepository(Vertx vertx, String dbFile) {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + dbFile)
                .put("driver_class", "org.sqlite.JDBC");

        client = JDBCClient.createShared(vertx, config, "MyDataSource");

        init();
    }

    public Single<ResultSet> getAll() {
        return client
                .rxGetConnection()
                .flatMap(conn -> conn
                        .rxQuery("SELECT * FROM files")
                        .doAfterTerminate(conn::close)
                );
    }


    private void init() {
        client.rxGetConnection()
                .flatMap(conn -> conn.rxExecute("CREATE TABLE IF NOT EXISTS files (name STRING, rndsuffix STRING, size INTEGER, packsize INTEGER)")
                        .doAfterTerminate(conn::close)
                );
    }
}
