package com.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnector {

    // Дефиниране на параметрите за връзка
    // Форматът е: jdbc:oracle:thin:@[host]:[port]:[SID]
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "system";
    private static final String PASSWORD = "oracle";

    public static Connection getConnection() throws SQLException {
        try {
            // Зареждане на драйвера
            Class.forName("oracle.jdbc.OracleDriver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC Driver not found!", e);
        }
    }
}
