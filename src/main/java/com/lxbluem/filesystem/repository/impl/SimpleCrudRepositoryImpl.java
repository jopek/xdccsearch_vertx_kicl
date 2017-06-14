package com.lxbluem.filesystem.repository.impl;

import com.lxbluem.filesystem.FileEntity;
import com.lxbluem.filesystem.repository.SimpleCrudRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class SimpleCrudRepositoryImpl implements SimpleCrudRepository<FileEntity, FileEntity> {


    private final JDBCClient client;

    public SimpleCrudRepositoryImpl(Vertx vertx, String dbFile) {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + dbFile)
                .put("driver_class", "org.sqlite.JDBC");

        client = JDBCClient.createShared(vertx, config, "MyDataSource");
        init();
    }

    @Override
    public Single<Void> save(FileEntity entity) {
        JsonArray params = new JsonArray()
                .add(entity.getName())
                .add(entity.getRndsuffix())
                .add(entity.getSize())
                .add(entity.getPacksize())
                .add(entity.getCreatedAt() > 0 ? entity.getCreatedAt() : System.currentTimeMillis());

        return client.rxGetConnection()
                .flatMap(conn -> conn.rxUpdateWithParams(SAVE_STATEMENT, params)
                        .map(r -> (Void) null)
                        .doAfterTerminate(conn::close)
                );

    }

    @Override
    public Single<Void> saveAll(List<FileEntity> entities) {
        List<JsonArray> insertParameters = entities.stream()
                .map(entity -> new JsonArray()
                        .add(entity.getName())
                        .add(entity.getRndsuffix())
                        .add(entity.getSize())
                        .add(entity.getPacksize())
                        .add(entity.getCreatedAt() > 0 ? entity.getCreatedAt() : System.currentTimeMillis())
                )
                .collect(toList());

        return client.rxGetConnection()
                .flatMap(conn ->
                        conn.rxBatchWithParams(SAVE_STATEMENT, insertParameters)
                                .map(r -> (Void) null)
                                .doAfterTerminate(conn::close)
                );
    }


    @Override
    public Single<Optional<FileEntity>> retrieveOne(FileEntity entity) {
        return client.rxGetConnection()
                .flatMap(conn -> {
                            JsonArray params = new JsonArray()
                                    .add(entity.getName())
                                    .add(entity.getRndsuffix())
                                    .add(entity.getPacksize());

                            return conn.rxQueryWithParams(RETRIEVE_STATEMENT, params)
                                    .map(ResultSet::getRows)
                                    .map(list -> {
                                        if (list.isEmpty()) {
                                            return Optional.<FileEntity>empty();
                                        } else {
                                            return Optional.of(list.get(0))
                                                    .map(this::mapToEntity);
                                        }
                                    })
                                    .doAfterTerminate(conn::close);
                        }
                );

    }

    @Override
    public Observable<FileEntity> retrieveAll() {
        return client.rxGetConnection()
                .flatMapObservable(connection ->
                        connection.rxQuery(GET_ALL_STATEMENT)
                                .map(ResultSet::getRows)
                                .flatMapObservable(Observable::from)
                                .map(this::mapToEntity)
                                .doAfterTerminate(connection::close)
                );
    }

    @Override
    public Single<Void> delete(FileEntity entity) {
        return client.rxGetConnection()
                .flatMap(conn -> {
                            JsonArray params = new JsonArray()
                                    .add(entity.getName())
                                    .add(entity.getRndsuffix())
                                    .add(entity.getPacksize());

                            return conn.rxUpdateWithParams(DELETE_STATEMENT, params)
                                    .map(updateResult -> (Void) null)
                                    .doAfterTerminate(conn::close);
                        }
                );
    }

    private void init() {
        client.rxGetConnection()
                .flatMap(conn -> conn.rxExecute(INIT_STATEMENT)
                        .doAfterTerminate(conn::close)
                )
                .subscribe();
    }

    private FileEntity mapToEntity(JsonObject jo) {
        return FileEntity.builder()
                .name(jo.getString("name"))
                .rndsuffix(jo.getString("rndsuffix"))
                .packsize(jo.getLong("packsize"))
                .size(jo.getLong("size"))
                .build();
    }


    // SQL statements

    private static final String INIT_STATEMENT = "CREATE TABLE IF NOT EXISTS files (" +
            "name STRING NOT NULL, " +
            "rndsuffix STRING NOT NULL, " +
            "size INTEGER NOT NULL, " +
            "packsize INTEGER NOT NULL, " +
            "created_at INTEGER NOT NULL " +
            "PRIMARY KEY (`id`)" +
            ")";

    private static final String GET_ALL_STATEMENT = "SELECT * FROM files";

    private static final String RETRIEVE_STATEMENT = "SELECT * FROM files WHERE name = ? AND rndsuffix = ? AND packsize = ?";

    private static final String DELETE_STATEMENT = "DELETE FROM files WHERE name = ? AND rndsuffix = ? AND packsize = ?";

    private static final String SAVE_STATEMENT = "INSERT INTO files (" +
            "name, rndsuffix, size, packsize, created_at) " +
            "VALUES (?, ?, ?, ?, ?)";

}
