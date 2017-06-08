package com.lxbluem.filesystem;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;

import java.sql.*;

public class FileNameRepository {
    public static void main(String[] args) {
        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");
            ResultSet rs = statement.executeQuery("select * from person");
            while (rs.next()) {
                // read the result set
                System.out.println("name = " + rs.getString("name"));
                System.out.println("id = " + rs.getInt("id"));
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }

    FileNameRepository(Vertx vertx) {

        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:sample.db")
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30);

        JDBCClient client = JDBCClient.createShared(vertx, config, "MyDataSource");

        client.getConnection(conn -> {
            if (conn.failed())
                throw new RuntimeException(conn.cause());

            client.getConnection(connectionAsyncResult -> {
                if (connectionAsyncResult.succeeded()) {
                    SQLConnection connection = connectionAsyncResult.result();

                    connection.query("SELECT * FROM person", res2 -> {
                        if (res2.succeeded()) {

                            io.vertx.ext.sql.ResultSet rs = res2.result();
                            System.out.println(rs.getRows());
                        }
                    });
                } else {
                    connectionAsyncResult.cause().printStackTrace();
                }
            });
        });
    }
}
