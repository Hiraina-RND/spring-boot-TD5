package com.spring_boot.TD5.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {
    public Connection getConnection() {
        try {
            String dbUrl =  "jdbc:postgresql://localhost:5432/mini_dish_db"; //System.getenv("DB_URL");
            String dbUser = "mini_dish_db_manager"; //System.getenv("DB_USERNAME");
            String dbPassword = "123456"; //System.getenv("DB_PASSWORD");

            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}