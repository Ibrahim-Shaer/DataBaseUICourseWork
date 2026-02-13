package com.controllers;

import com.util.DatabaseException;
import com.util.Validator;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import com.database.DatabaseService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML
    private TableView<ObservableList<String>> tableView;

    private com.database.DatabaseService dbService = new com.database.DatabaseService();

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
        dbService.loadTableData(tableView, currentTable);
    }

    @FXML
    private void handleShowPeople() {
        currentTable = "Person";
        currentIdColumn = "PERSON_ID";
        dbService.loadTableData(tableView, currentTable);
    }

    @FXML
    private void handleShowProperties() {
        currentTable = "Properties";
        currentIdColumn = "PROPERTY_ID";
        dbService.loadTableData(tableView, currentTable);
    }

    @FXML
    private void handleListings() {
        currentTable = "Listings";
        currentIdColumn = "LISTING_ID";
        dbService.loadTableData(tableView, currentTable);
    }

    @FXML
    private void handleDelete() {
        ObservableList<String> selectedRow = tableView.getSelectionModel().getSelectedItem();

        if(selectedRow == null) {
            showError("Моля изберете ред за изтриване.");
            return;
        }

        String idValue = selectedRow.get(0);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Потвърждение");
        confirm.setContentText("Сигурни ли сте, че искате да изтриете записа?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            dbService.deleteRecord(currentTable, currentIdColumn, idValue);
            dbService.loadTableData(tableView, currentTable);
        } catch (Exception e) {
            showError("Не може да се изтрие записът.\nВъзможна връзка с друга таблица.");
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
            // Send to database
            if (currentTable.equals("Person")) {
                int emailIndex = getColumnIndex("EMAIL");
                int phoneIndex = getColumnIndex("PHONE_NUMBER");

                if (emailIndex != -1) {
                    String email = result.get(emailIndex);
                    if (!Validator.isValidEmail(email)) {
                        showError("Невалиден имейл!");
                        return;
                    }
                }

                if (phoneIndex != -1) {
                    String phone = result.get(phoneIndex);
                    if (!Validator.isValidPhone(phone)) {
                        showError("Невалиден телефон!");
                        return;
                    }
                }
            }

            try {
                dbService.insertDynamic(currentTable, result);
                dbService.loadTableData(tableView,currentTable);
            } catch (Exception e) {
                showError("Грешка при запис в базата!");
            }
        });
    }

    @FXML
    private void handleComplexQueries() {
        List<String> choices = Arrays.asList(
                "1. Всички апартаменти с техните собственици",
                "2. Агенти с общата стойност на техните сделки",
                "3. Най-търсените предпочитания от клиентите",
                "4. Имоти без нито едно посещение",
                "5. Най-скъпа продажба (Агент и Клиент)"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Сложни справки");
        dialog.showAndWait().ifPresent(choice -> {
            try {
                int index = Character.getNumericValue(choice.charAt(0));
                dbService.loadComplexQuery(tableView, index);
                currentTable = "QUERY_RESULT";
            } catch (Exception e) {
                showError("Грешка при изпълнение на справката: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

   /* private void executeComplexQuery(String sql) {
        // Use loadData to show result of handleComplexQueries
        dbService.loadData(tableView, sql);
        currentTable = "QUERY_RESULT";
    }*/

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Грешка");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private int getColumnIndex(String columnName) {
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            if (tableView.getColumns().get(i).getText().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }


}
