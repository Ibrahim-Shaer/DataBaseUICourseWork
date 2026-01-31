package util;

import com.database.DataBaseConnector;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.*;
import java.util.List;

public class DatabaseService {
    public void loadData(TableView<ObservableList<String>> tableView, String query) {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();


        tableView.getItems().clear();
        tableView.getColumns().clear();

        try (Connection conn = DataBaseConnector.getConnection();
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
            e.printStackTrace();
            System.err.println("Грешка при комуникация с Oracle DB!");
        }
    }


    public void deleteRecord(String tableName, String idColumnName, String idValue) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?";
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, idValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            System.err.println("Не мога да генерирам ID: " + e.getMessage());
        }
        return 1;
    }
}
