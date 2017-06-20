package com.lxbluem.filesystem;

import org.junit.Test;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class LearningTest {

    private Connection connection;

    @Test
    public void createDb() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");

        try {
            Statement statement = connection.createStatement();

            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");
            ResultSet rs = statement.executeQuery("select * from person");
            while (rs.next()) {
                // read the result set
                System.out.println("packname = " + rs.getString("packname"));
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


        connection = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");
        ResultSet rs = connection.createStatement().executeQuery("select * from person");
        while (rs.next()) {
            // read the result set
            System.out.println("packname = " + rs.getString("packname"));
            System.out.println("id = " + rs.getInt("id"));
        }

    }

    @Test
    public void name() throws Exception {

        String packname = "/Users/alex/dev/priv/xdccsearch-vertx-kicl/downloads/file3a";

        int lastIndexOf = packname.lastIndexOf("downloads/");
        String substring = packname.substring(lastIndexOf, packname.length());
        System.out.println(substring);
    }
}
