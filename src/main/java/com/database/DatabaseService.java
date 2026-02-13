package com.database;

import com.database.DataBaseConnector;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.*;
import java.util.List;

public class DatabaseService {

    private static Connection sharedConnection;
    private Connection getConnection() throws SQLException {
        if (sharedConnection == null || sharedConnection.isClosed()) {
            sharedConnection = DataBaseConnector.getConnection();
        }
        return sharedConnection;
    }


    public void loadTableData(TableView<ObservableList<String>> tableView, String tableName) {
        String query = "SELECT * FROM " + tableName;
        loadData(tableView, query);
    }

    public void loadComplexQuery(TableView<ObservableList<String>> tableView, int queryIndex) {
        String sql = switch (queryIndex) {
            // 1. Апартаменти и техните собственици
            case 1 -> "SELECT p.FIRST_NAME, p.LAST_NAME, pr.LOCATION, a.NUMBER_OF_ROOMS " +
                    "FROM Person p JOIN Property_Owners po ON p.PERSON_ID = po.PERSON_ID " +
                    "JOIN Properties pr ON po.PROPERTY_ID = pr.PROPERTY_ID " +
                    "JOIN Apartment a ON pr.PROPERTY_ID = a.PROPERTY_ID";

            // 2. Агенти и стойност на сделки (Тук трябва да свържем Deals с Visits, за да намерим агента)
            case 2 -> "SELECT p.FIRST_NAME, p.LAST_NAME, SUM(sd.FINAL_PRICE) as TOTAL_SALES " +
                    "FROM Person p JOIN Agents ag ON p.PERSON_ID = ag.PERSON_ID " +
                    "JOIN Visits v ON ag.PERSON_ID = v.AGENT_ID " +
                    "JOIN Successful_Deals sd ON v.PROPERTY_ID = sd.PROPERTY_ID " +
                    "GROUP BY p.FIRST_NAME, p.LAST_NAME";

            // 3. Най-търсени предпочитания
            case 3 -> "SELECT pr.PREFERENCE_TYPE, COUNT(*) as TIMES_CHOSEN " +
                    "FROM Preferences pr JOIN Client_Preferences cp ON pr.PREFERENCE_ID = cp.PREFERENCE_ID " +
                    "GROUP BY pr.PREFERENCE_TYPE ORDER BY TIMES_CHOSEN DESC";

            // 4. Имоти без посещения
            case 4 -> "SELECT PROPERTY_ID, PRICE, LOCATION FROM Properties " +
                    "WHERE PROPERTY_ID NOT IN (SELECT DISTINCT PROPERTY_ID FROM Visits)";

            // 5. Най-скъпа продажба (Агент и Клиент)
            case 5 -> "SELECT ap.LAST_NAME as Agent, cp.LAST_NAME as Client, pr.LOCATION, sd.FINAL_PRICE " +
                    "FROM Successful_Deals sd " +
                    "JOIN Properties pr ON sd.PROPERTY_ID = pr.PROPERTY_ID " +
                    "JOIN Visits v ON pr.PROPERTY_ID = v.PROPERTY_ID " +
                    "JOIN Person ap ON v.AGENT_ID = ap.person_id " +
                    "JOIN Person cp ON v.CLIENT_ID = cp.person_id " +
                    "WHERE sd.FINAL_PRICE = (SELECT MAX(FINAL_PRICE) FROM Successful_Deals) " +
                    "FETCH FIRST 1 ROWS ONLY"; // Oracle специфично за една редица
            default -> throw new IllegalArgumentException("Невалиден индекс: " + queryIndex);
        };
        loadData(tableView, sql);
    }


    public void loadData(TableView<ObservableList<String>> tableView, String query) {

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        tableView.getItems().clear();
        tableView.getColumns().clear();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // 1. Dynamicly create columns
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                final int j = i;
                TableColumn<ObservableList<String>, String> col =
                        new TableColumn<>(rs.getMetaData().getColumnName(i + 1));

                col.setCellValueFactory(param -> {

                    ObservableList<String> row = param.getValue();
                    String value = (row != null && j < row.size()) ? row.get(j) : "";
                    return new SimpleStringProperty(value);
                });

                tableView.getColumns().add(col);
            }

            // 2. Add the data
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    Object val = rs.getObject(i);
                    row.add(val != null ? val.toString() : "NULL");
                }
                data.add(row);
            }
            tableView.setItems(data);

        } catch (SQLException e) {
            throw new RuntimeException("Грешка при зареждане на данни: " + e.getMessage());
        }
    }


    public void deleteRecord(String tableName, String idColumnName, String idValue) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?";
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, idValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Грешка при изтриване. Вероятно записът е свързан с друга таблица.");
        }
    }

    public void insertDynamic(String tableName, List<String> values) {
        try (Connection conn = DataBaseConnector.getConnection()) {

            String selectSql = "SELECT * FROM " + tableName + " WHERE 1=0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(selectSql);
            int columnCount = rs.getMetaData().getColumnCount();


            StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
            for (int i = 0; i < columnCount; i++) {
                sql.append(i == 0 ? "?" : ", ?");
            }
            sql.append(")");


            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < values.size(); i++) {
                pstmt.setString(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
            System.out.println("Успешно добавяне в " + tableName);

        } catch (SQLException e) {
            throw new RuntimeException("Грешка при вмъкване на данни: " + e.getMessage());
        }
    }

    public int getNextId(String tableName, String idColumnName) {

        String sql = "SELECT MAX(" + idColumnName + ") FROM " + tableName;
        try (Connection conn = com.database.DataBaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int maxId = rs.getInt(1);
                return maxId + 1;
            }
        } catch (SQLException e) {
            System.err.println("Грешка при генериране на ID: " + e.getMessage());
        }
        return 1;
    }
}
