package com.controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML
    private TableView<ObservableList<String>> tableView;

    private util.DatabaseService dbService = new util.DatabaseService();

    private String currentTable = "Properties";
    private String currentIdColumn = "PROPERTY_ID";

    @FXML
    public void initialize() {
        handleShowProperties();
    }

    @FXML
    private void handleShowDeals() {
        currentTable = "Successful_Deals";
        currentIdColumn = "DEAL_ID";
        dbService.loadData(tableView, "SELECT * FROM Successful_Deals");
    }
    @FXML
    private void handleShowPeople() {
        currentTable = "Person";
        currentIdColumn = "PERSON_ID";
        dbService.loadData(tableView, "SELECT * FROM Person");
    }

    @FXML
    private void handleShowProperties() {
        currentTable = "Properties";
        currentIdColumn = "PROPERTY_ID";
        dbService.loadData(tableView, "SELECT * FROM Properties");
    }

    @FXML
    private void handleListings() {
        currentTable = "Listings";
        currentIdColumn = "LISTING_ID";
        dbService.loadData(tableView, "SELECT * FROM Listings");
    }

    @FXML
    private void handleDelete() {
        ObservableList<String> selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow != null) {

            String idValue = selectedRow.get(0);
            dbService.deleteRecord(currentTable, currentIdColumn, idValue);


            dbService.loadData(tableView, "SELECT * FROM " + currentTable);
        }
    }

    @FXML
    private void handleAddDynamic() {
        // Taking the columns from the current table
        ObservableList<TableColumn<ObservableList<String>, ?>> columns = tableView.getColumns();

        if (columns.isEmpty()) return;

        // Create a dialog window
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Добавяне в " + currentTable);
        dialog.setHeaderText("Въведете данни за новия запис:");

        ButtonType addButtonType = new ButtonType("Запази", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<TextField> fields = new ArrayList<>();

        String idColumnName = columns.get(0).getText();
        int nextId = dbService.getNextId(currentTable, idColumnName);
        //For every column we create a new field(auto increment ID)
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i).getText();
            TextField field = new TextField();
            if(i == 0)
            {
                field.setText(String.valueOf(nextId));
                field.setEditable(false);
                field.setStyle("-fx-background-color: white; -fx-font-weight: bold;");
            }

            field.setPromptText(columnName);
            grid.add(new Label(columnName + ":"), 0, i);
            grid.add(field, 1, i);
            fields.add(field);
        }

        dialog.getDialogPane().setContent(grid);


        //Convert result to list
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return fields.stream().map(TextField::getText).collect(Collectors.toList());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            // Send to data base
            dbService.insertDynamic(currentTable, result);
            dbService.loadData(tableView, "SELECT * FROM " + currentTable);
        });
    }

    @FXML
    private void handleComplexQueries() {
        List<String> choices = Arrays.asList(
                "1. Всички апартаменти с техните собственици",
                "2. Агенти с общата стойност на техните сделки",
                "3. Най-търсените предпочитания от клиентите",
                "4. Имоти без нито едно посещение (Visits)",
                "5. Кой агент е продал най-скъпия имот на кой клиент"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Сложни заявки");
        dialog.setHeaderText("Изберете заявка:");
        dialog.setContentText("Заявка:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            if (choice.startsWith("1")) {
                executeComplexQuery("SELECT p.FIRST_NAME, p.LAST_NAME, pr.LOCATION, a.NUMBER_OF_ROOMS " +
                        "FROM Person p " +
                        "JOIN Property_Owners po ON p.PERSON_ID = po.PERSON_ID " +
                        "JOIN Properties pr ON po.PROPERTY_ID = pr.PROPERTY_ID " +
                        "JOIN Apartment a ON pr.PROPERTY_ID = a.PROPERTY_ID");
            } else if (choice.startsWith("2")) {
                executeComplexQuery("SELECT p.FIRST_NAME, p.LAST_NAME, SUM(sd.FINAL_PRICE) as TOTAL_SALES " +
                        "FROM Person p " +
                        "JOIN Agents a ON p.PERSON_ID = a.PERSON_ID " +
                        "JOIN Visits v ON a.PERSON_ID = v.AGENT_ID " +
                        "JOIN Successful_Deals sd ON v.PROPERTY_ID = sd.PROPERTY_ID " +
                        "GROUP BY p.FIRST_NAME, p.LAST_NAME");
            } else if (choice.startsWith("3")) {
                executeComplexQuery("SELECT PREFERENCE_TYPE, COUNT(*) as TIMES_CHOSEN " +
                        "FROM Preferences pr " +
                        "JOIN Client_Preferences cp ON pr.PREFERENCE_ID = cp.PREFERENCE_ID " +
                        "GROUP BY PREFERENCE_TYPE ORDER BY TIMES_CHOSEN DESC");
            } else if (choice.startsWith("4")) {
                executeComplexQuery("SELECT PROPERTY_ID, PRICE, LOCATION FROM Properties " +
                        "WHERE PROPERTY_ID NOT IN (SELECT PROPERTY_ID FROM Visits)");
            }
            else if (choice.startsWith("5")) {
                executeComplexQuery("SELECT a_p.LAST_NAME as Agent, c_p.LAST_NAME as Client, pr.LOCATION, sd.FINAL_PRICE\n" +
                        "FROM Successful_Deals sd\n" +
                        "JOIN Properties pr ON sd.PROPERTY_ID = pr.PROPERTY_ID\n" +
                        "JOIN Visits v ON pr.PROPERTY_ID = v.PROPERTY_ID\n" +
                        "JOIN Person a_p ON v.AGENT_ID = a_p.PERSON_ID\n" +
                        "JOIN Person c_p ON v.CLIENT_ID = c_p.PERSON_ID\n" +
                        "WHERE sd.FINAL_PRICE = (SELECT MAX(FINAL_PRICE) FROM Successful_Deals)");
            }
        });
    }

    private void executeComplexQuery(String sql) {
        // Use loadData to show result of handleComplexQueries
        dbService.loadData(tableView, sql);
        currentTable = "QUERY_RESULT";
    }
}
