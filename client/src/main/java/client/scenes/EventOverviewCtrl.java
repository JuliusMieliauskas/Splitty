/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client.scenes;

import client.ExpenseAction;
import client.LanguageChoiceBox;
import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EventUtils;
import client.utils.ExpenseUtils;
import client.utils.TagColorManagerUtils;
import client.utils.UndoUtils;
import client.utils.UserUtils;
import client.utils.WebSocketUtils;
import com.google.inject.Inject;
import commons.Event;
import commons.EventUpdate;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventOverviewCtrl implements Initializable {
    @FXML
    private VBox eventNameWrapper;
    @FXML
    private VBox editEventNameWrapper;
    @FXML
    private Label filtersLabel;
    @FXML
    private MenuButton eventUsersMenuButton;
    @FXML
    private MenuButton tagFilterMenuButton;
    @FXML
    private TableView<Expense> expenseTable;
    @FXML
    private TableColumn<Expense, String> dateColumn;
    @FXML
    private TableColumn<Expense, String> descriptionColumn;
    @FXML
    private TableColumn<Expense, Double> amountColumn;
    @FXML
    private TableColumn<Expense, String> tagColumn;
    @FXML
    private Button deleteExpenseButton;

    @FXML
    private Label participantList;
    @FXML
    private Label eventName;
    @FXML
    private MenuButton tagMenu;
    @FXML
    private MenuButton tagColorMenu;
    @FXML
    private TextField tagTextField;
    @FXML
    private Label selectOrCreateTag;
    @FXML
    private Button submitTagButton;
    @FXML
    private RadioButton showAll, showFromUser, showIncludingUser;
    @FXML
    private Label totalAmountNum;
    @FXML
    private TextField eventNameTextField;
    @FXML
    private Button settleDebtButton;
    @FXML
    private Button addExpenseButton;
    @FXML
    private Label inviteCodeNum;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private Label expensesLabel;
    @FXML
    private Button sendInviteButton;
    @FXML
    private Button statsButton;
    @FXML
    private Button returnButton;
    @FXML
    private Label inviteCodeLabel;
    @FXML
    private VBox languageChoiceBoxContainer;
    @FXML
    private Button downloadJsonButton;

    public LanguageChoiceBox languageChoiceBox;
    private final MainCtrl mainCtrl;
    private WebSocketUtils webSocketUtils;
    private String selectedTag;
    private String selectedUserName;
    private Long selectedExpenseId;
    private int lastSelectedIndex = -1;
    private String selectedTagFilter;
    private Map<String, String> tagColors = new HashMap<>();
    private final List<String> allColors = new ArrayList<>(Arrays.asList("Red", "Green", "Blue", "Orange", "Yellow",
            "Purple", "Pink", "Cyan", "Black", "Brown", "Gray")); //If you want to add colors add it to resourcebundle as well
    private Map<String, String> colorOtherLanguageToEnglish = new HashMap<>();

    /**
     * injects mainCtrl
     *
     * @param mainCtrl mainCtrl of scenes
     */
    @Inject
    public EventOverviewCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.languageChoiceBox = new LanguageChoiceBox(mainCtrl);
    }

    public void initWebSocket(String url) {
        webSocketUtils = new WebSocketUtils(url, this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(addExpenseButton, "create");
        mainCtrl.setButtonIcon(returnButton, "back");

        initTableView();
        Platform.runLater(this::updateLanguage);
        Platform.runLater(() -> {
            Scene scene = eventName.getScene();
            if (scene != null) {
                scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        Node clicked = mouseEvent.getPickResult().getIntersectedNode();
                        while (clicked != null) { // check if eventNameTextField was clicked
                            if (clicked == eventNameTextField) {
                                return;
                            }
                            clicked = clicked.getParent();
                        }
                        cancelEventNameChange();
                    }
                });
                scene.setOnKeyPressed(this::addShortcuts);
            }
        });
        ChangeListener<Boolean> radioChangeListener = (observable, oldValue, newValue) -> {
            if (newValue) {
                filterExpenseList();
            }
        };

        eventNameTextField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    confirmEventNameChange();
                }
            }
        });

        showAll.selectedProperty().addListener(radioChangeListener);
        showFromUser.selectedProperty().addListener(radioChangeListener);
        showIncludingUser.selectedProperty().addListener(radioChangeListener);

        languageChoiceBoxContainer.getChildren().add(languageChoiceBox);
        languageChoiceBoxContainer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                languageChoiceBox.show();
            }
        });
    }

    private void addShortcuts(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            returnToStartScreen();
            event.consume();
        }
        if (event.getCode() == KeyCode.F2) {
            showEventNameTextField();
            event.consume();
        }
        if (event.getCode() == KeyCode.P) {
            editParticipants();
        }
        if (event.getCode() == KeyCode.E) {
            addExpense();
        }
        if (event.getCode() == KeyCode.D) {
            settleDebt();
        }
        if (event.getCode() == KeyCode.S) {
            showStats();
        }
        checkUndoRedo(event);
    }

    private void checkUndoRedo(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.Z) {
            UndoUtils.undo();
            refreshTableView();
        }
        if (event.isControlDown() && event.getCode() == KeyCode.Y) {
            UndoUtils.redo();
            refreshTableView();
        }
    }

    /**
     * Called by the websocket util when the event updates
     *
     * @param eventUpdate The type of update to the event
     */
    public void handleEventUpdate(EventUpdate eventUpdate) {
        Platform.runLater(() -> {
            switch (eventUpdate.getAction()) {
                case ADDED_USER, REMOVED_USER, UPDATED_USER -> {
                    List<User> users = EventUtils.getUsersOfEvent(ConfigUtils.getCurrentEvent().getId());
                    fillParticipants(users);
                }
                case ADDED_EXPENSE, REMOVED_EXPENSE, REMOVED_ALL_EXPENSES, UPDATED_EXPENSE, UPDATED_EVENT -> {
                    ConfigUtils.setCurrentEvent(EventUtils.getEventById(ConfigUtils.getCurrentEvent().getId()));
                    init();
                    eventName.setText(EventUtils.getEventById(ConfigUtils.getCurrentEvent().getId()).getTitle() + " \u270E");
                    expenseTable.refresh();
                }
                case DELETED_EVENT -> {
                    mainCtrl.showAlert(LanguageController.getInstance().getString("event was deleted") +
                            ": " + ConfigUtils.getCurrentEvent().getTitle(), Alert.AlertType.INFORMATION);
                    ConfigUtils.removeEvent(ConfigUtils.getCurrentEvent().getId());
                    ConfigUtils.tryLoadConfig();
                    webSocketUtils.unsubscribe();
                    mainCtrl.showStartScreen();
                }
            }
        });
    }

    /**
     * Takes an event and displays its event page.
     */
    public void init() {
        resetOnPageChange();
        tagColors = new HashMap<>();
        tagColors = TagColorManagerUtils.readTagColorsFromFile(ConfigUtils.getCurrentEvent().getId());
        inviteCodeNum.setText(ConfigUtils.getCurrentEvent().getInviteCode());
        fillTableView(ConfigUtils.getCurrentEvent());
        addColorsToTagColorMenu();
        addTagsToTagFilterMenuButton();
        applyFilters();
        webSocketUtils.subscribe(ConfigUtils.getCurrentEvent().getId());

        refreshTableView();
        eventName.setVisible(true);
        editEventNameWrapper.setVisible(false);
        sendInviteButton.setDisable(ConfigUtils.getUserEmail() == null);
    }


    private void initTableView() {
        tagColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(translateToSelectedLanguage(Optional.ofNullable(cellData.getValue().getTag()).orElse("noTag"))));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(expenseToString(cellData.getValue())));
        descriptionColumn.setSortable(false);
        tagColumn.setSortable(false); //changed: maybe it's nice to have?
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        setupCellFactory(descriptionColumn, true);
        setupCellFactory(tagColumn, false);
        amountColumn.setCellFactory(column -> new TableCell<Expense, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    double exchangedValue = item * ConfigUtils.getExchangeRate();
                    setText(String.format("%.2f", exchangedValue));
                }
            }
        });
        expenseTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                confirmDeleteExpense();
            } else if (e.getCode() == KeyCode.ENTER) {
                showExpenseView();
            }
            if (e.getCode() == KeyCode.F2) {
                showEventNameTextField();
                e.consume();
            }
        });

    }

    private void setupCellFactory(TableColumn<Expense, String> column, boolean isDescriptionColumn) {
        column.setCellFactory(col -> new TableCell<Expense, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Expense expense = getTableView().getItems().get(getIndex());
                    String tag = expense.getTag() != null ? expense.getTag().toLowerCase() : "noTag";
                    String color = tagColors.get(tag);
                    if (color == null && !"noTag".equals(tag)) {
                        Set<String> usedColors = new HashSet<>(tagColors.values());
                        color = getRandomColor(usedColors);
                        tagColors.put(tag, color);
                        TagColorManagerUtils.writeTagColorsToFile(ConfigUtils.getCurrentEvent().getId(), tagColors);
                    }
                    color = color != null ? color : "black";
                    String textColor = Arrays.asList("Orange", "Yellow", "Pink", "Cyan").contains(color) ? "black" : "white";
                    setStyle("-fx-background-color: " + color + ";" + "-fx-text-fill: " + textColor);
                }
            }
        });
    }

    /**
     * Fills the listview with all the expenses
     *
     * @param event of the expenses that are displayed
     */
    public void fillTableView(Event event) {
        if (event == null || ConfigUtils.getCurrentEvent() == null) {
            return;
        }
        String currency = ConfigUtils.getCurrency();
        amountColumn.setText(LanguageController.getInstance().getString("Amount") + " (" + currency + ")");
        expenseTable.setPlaceholder(new Label(LanguageController.getInstance().getString("No expenses available")));
        List<Expense> expenses = EventUtils.getExpensesOfEvent(ConfigUtils.getCurrentEvent().getId());
        for (Expense expense : expenses) {
            try {
                String tag = expense.getTag().toLowerCase();
                if (!tagColors.containsKey(tag)) {
                    tagColors.put(tag, null);
                    TagColorManagerUtils.writeTagColorsToFile(ConfigUtils.getCurrentEvent().getId(), tagColors);
                }

            } catch (NullPointerException e) {
                System.out.println("There's an expense without a tag");
            }
        }
        tagMenu.getItems().clear();
        ObservableList<Expense> eventExpenses = FXCollections.observableList(expenses);
        expenseTable.setItems(eventExpenses);
        List<User> users = EventUtils.getUsersOfEvent(ConfigUtils.getCurrentEvent().getId());
        fillParticipants(users);
        eventName.setText(ConfigUtils.getCurrentEvent().getTitle() + " \u270E");
        expenseTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Expense>() {
            @Override
            public void changed(ObservableValue<? extends Expense> observable, Expense oldValue, Expense newValue) {
                if (newValue == null) {
                    return;
                }
                lastSelectedIndex = expenseTable.getSelectionModel().getSelectedIndex();
                setUpTagMenuItems(expenses, newValue);
            }
        });

        expenseTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !expenseTable.getSelectionModel().isEmpty()) {
                showExpenseView();

            }
        });
        eventUsersMenuButton.getItems().forEach(menuItem -> menuItem.setOnAction(this::switchUserOfExpenses));
        eventUsersMenuButton.setText(LanguageController.getInstance().getString("Select User"));
        restoreSelection();
        applyFilters();
    }

    private void setUpTagMenuItems(List<Expense> expenses, Expense newValue) {
        tagMenu.getItems().clear();
        tagMenu.setVisible(true);
        tagTextField.setVisible(true);
        selectOrCreateTag.setVisible(true);
        submitTagButton.setVisible(true);
        tagColorMenu.setVisible(true);
        deleteExpenseButton.setVisible(true);
        String tagText = Optional.ofNullable(newValue.getTag()).filter(tag -> !tag.isEmpty()).orElse("noTag");
        tagMenu.setText(translateToSelectedLanguage(tagText));
        Set<String> uniqueTags = expenses.stream()
                .map(Expense::getTag)
                .filter(Objects::nonNull)
                .filter(tag -> !((tag.equals("food")) || (tag.equals("entrance_fees")) || (tag.equals("travel"))))
                .collect(Collectors.toSet());
        uniqueTags.add(LanguageController.getInstance().getString("noTag"));
        List<String> requiredTags = Arrays.asList(translateToSelectedLanguage("food"),
                translateToSelectedLanguage("entrance_fees"), translateToSelectedLanguage("travel"));
        uniqueTags.addAll(requiredTags);
        uniqueTags.forEach(tag -> tagMenu.getItems().add(new MenuItem(tag)));
        tagMenu.getItems().forEach(menuItem -> menuItem.setOnAction(EventOverviewCtrl.this::switchTag));
        tagColorMenu.getItems().forEach(menuItem -> menuItem.setOnAction(EventOverviewCtrl.this::setColorForTag));
        String tagColorStr = tagColors.get(newValue.getTag());
        //tagColorMenu.setText(tagColorStr != null ? translateTag(tagColorStr) : "Select Color");
        if ((tagColorStr == null) || "gray".equals(tagColorStr)) {
            tagColorMenu.setText(LanguageController.getInstance().getString("Select Color"));
        } else {
            tagColorMenu.setText(LanguageController.getInstance().getString(tagColorStr));
        }
        selectedTag = newValue.getTag();
        selectedExpenseId = newValue.getId();
    }

    private String translateToSelectedLanguage(String tagText) {
        if (tagText == null || tagText.isEmpty() || "noTag".equals(tagText)) {
            return LanguageController.getInstance().getString("noTag");
        }
        try {
            return LanguageController.getInstance().getString(tagText);
        } catch (Exception e) {
            return tagText;
        }
    }


    private void fillParticipants(List<User> users) {
        eventUsersMenuButton.getItems().clear();
        StringBuilder participants = new StringBuilder();
        users.forEach(user -> {
            if (!participants.isEmpty()) {
                participants.append(", ");
            }
            participants.append(user.getUsername());
            eventUsersMenuButton.getItems().add(new MenuItem(user.getUsername()));
        });
        if (participants.isEmpty()) {
            participantList.setText("(" + LanguageController.getInstance().getString("No participants") + ") \u270E");
        } else {
            participantList.setText(participants + " \u270E");
        }
    }

    private void setColorForTag(ActionEvent e) {
        MenuItem source = (MenuItem) e.getSource();
        String localizedColorName = source.getText();
        String selectedColor = colorOtherLanguageToEnglish.get(localizedColorName);

        //String selectedColor = ((MenuItem) e.getSource()).getText();
        String selectedTagEnglish = translateToEnglish(selectedTag);

        if (localizedColorName != null && !localizedColorName.isEmpty() && selectedTagEnglish != null && !selectedTagEnglish.isEmpty()) {
            tagColorMenu.setText(LanguageController.getInstance().getString(selectedColor));
            tagColors.put(selectedTagEnglish, selectedColor);
            TagColorManagerUtils.writeTagColorsToFile(ConfigUtils.getCurrentEvent().getId(), tagColors);
        } else {
            tagColorMenu.setText(LanguageController.getInstance().getString("Select Color"));
            mainCtrl.showAlert(LanguageController.getInstance().getString("No color for no tag"), Alert.AlertType.INFORMATION);
        }
        System.out.println(tagColors.get(selectedTag));
        refreshTableView();
        applyFilters();
    }

    private void switchTag(ActionEvent e) {
        tagMenu.setText(((MenuItem) e.getSource()).getText());
        selectedTag = ((MenuItem) e.getSource()).getText();
        System.out.println(selectedTag + "was chosen as tag");
        selectedTag = translateToSelectedLanguage("noTag").equalsIgnoreCase(selectedTag) ? null : selectedTag;
        String newTag = translateToEnglish(selectedTag);
        try {
            Expense oldExpense = ExpenseUtils.getExpenseById(selectedExpenseId);
            ExpenseUtils.changeTagOfExpense(selectedExpenseId, newTag);
            Expense newExpense = ExpenseUtils.getExpenseById(selectedExpenseId);
            UndoUtils.addAction(new ExpenseAction(ExpenseAction.Type.UPDATE, oldExpense, newExpense));
        } catch (FailedRequestException ex) {
            mainCtrl.showAlert("Could not change tag of expense:\n\n" + ex.getReason(), Alert.AlertType.ERROR);
        }

        if (newTag == null) {
            System.out.println("selected tag is somehow null");
            tagColorMenu.setText(LanguageController.getInstance().getString("Select Color"));
            refreshTableView();
            applyFilters();
            expenseTable.refresh();
            return;
        }
        String tagColorStr = tagColors.get(newTag);
        if (tagColorStr != null) {
            tagColorMenu.setText(LanguageController.getInstance().getString(tagColorStr));
        } else {
            tagColorMenu.setText(LanguageController.getInstance().getString("Select Color"));
        }

        boolean tagExists = tagFilterMenuButton.getItems().stream()
                .anyMatch(menuItem -> menuItem.getText().equals(translateToSelectedLanguage(newTag)));
        if (!tagExists && selectedTag != null && !selectedTag.isEmpty()) {
            MenuItem newTagFilterMenuItem = new MenuItem(LanguageController.getInstance().getString(newTag));
            newTagFilterMenuItem.setOnAction(this::applyTagFilter);
            tagFilterMenuButton.getItems().add(newTagFilterMenuItem);
        }
        refreshTableView();
        applyFilters();
        expenseTable.refresh();
    }

    private String translateToEnglish(String selectedTag) {
        String newTag = selectedTag;
        if (LanguageController.getInstance().getString("food").equals(newTag)) {
            newTag = "food";
        }
        if (LanguageController.getInstance().getString("entrance_fees").equals(newTag)) {
            newTag = "entrance_fees";
        }
        if (LanguageController.getInstance().getString("travel").equals(newTag)) {
            newTag = "travel";
        }
        return newTag;
    }


    /**
     * Adds new tag to the tag selection menu button
     */
    public void addNewTag() {
        if (tagTextField == null) {
            System.out.println("tag is null");
            return;
        }
        String newTag = tagTextField.getText().trim();
        boolean tagExists = tagMenu.getItems().stream()
                .anyMatch(menuItem -> menuItem.getText().equalsIgnoreCase(newTag));
        if (!tagExists) {
            tagColors.put(newTag, null);
            TagColorManagerUtils.writeTagColorsToFile(ConfigUtils.getCurrentEvent().getId(), tagColors);
            MenuItem newTagMenuItem = new MenuItem(newTag);
            newTagMenuItem.setOnAction(this::switchTag);
            tagMenu.getItems().add(newTagMenuItem);
            MenuItem newTagFilterMenuItem = new MenuItem(newTag);
            newTagFilterMenuItem.setOnAction(this::applyTagFilter);
            tagFilterMenuButton.getItems().add(newTagFilterMenuItem);
            tagTextField.clear();
        }
    }

    private void addTagsToTagFilterMenuButton() {
        tagFilterMenuButton.getItems().clear();
        MenuItem allTags = new MenuItem(LanguageController.getInstance().getString("allTags"));
        MenuItem noTag = new MenuItem(LanguageController.getInstance().getString("noTag"));
        tagFilterMenuButton.getItems().add(allTags);
        tagFilterMenuButton.getItems().add(noTag);
        Set<String> uniqueTags = EventUtils.getExpensesOfEvent(ConfigUtils.getCurrentEvent().getId())
                .stream()
                .map(Expense::getTag)
                .filter(Objects::nonNull)
                .map(this::translateToSelectedLanguage)
                .collect(Collectors.toSet());
        uniqueTags.forEach(tag -> tagFilterMenuButton.getItems().add(new MenuItem(tag)));
        tagFilterMenuButton.getItems().forEach(menuItem -> menuItem.setOnAction(EventOverviewCtrl.this::applyTagFilter));
        tagFilterMenuButton.setText(LanguageController.getInstance().getString("allTags"));
        applyTagFilter(new ActionEvent(allTags, null));
    }

    private void switchUserOfExpenses(ActionEvent e) {
        selectedUserName = ((MenuItem) e.getSource()).getText();
        eventUsersMenuButton.setText(selectedUserName);
        showIncludingUser.setDisable(false);
        showFromUser.setDisable(false);
        applyFilters();
    }

    private void applyTagFilter(ActionEvent e) {
        this.selectedTagFilter = ((MenuItem) e.getSource()).getText();
        tagFilterMenuButton.setText(this.selectedTagFilter);
        applyFilters();
    }

    public void filterExpenseList() {
        applyFilters();
    }

    private void applyFilters() {
        Stream<Expense> filteredStream = EventUtils.getExpensesOfEvent(ConfigUtils.getCurrentEvent().getId()).stream();

        filteredStream = filterUser(filteredStream);
        if (!LanguageController.getInstance().getString("allTags").equals(selectedTagFilter)) {
            final String currentTagFilter = translateToEnglish(selectedTagFilter);
            filteredStream = filteredStream.filter(expense ->
                    LanguageController.getInstance().getString("noTag").equals(currentTagFilter) ?
                            expense.getTag() == null || expense.getTag().isEmpty() :
                            expense.getTag() != null && expense.getTag().equals(currentTagFilter)
            );
        }
        ObservableList<Expense> filteredExpenses = FXCollections.observableArrayList(filteredStream.collect(Collectors.toList()));
        expenseTable.setItems(filteredExpenses);
        double totalSum = filteredExpenses.stream().mapToDouble(Expense::getAmount).sum();
        totalAmountNum.setText(String.format("%.2f", totalSum * ConfigUtils.getExchangeRate()) + " " + ConfigUtils.getCurrency());
    }

    private Stream<Expense> filterUser(Stream<Expense> filteredStream) {
        if (selectedUserName != null && !selectedUserName.isEmpty()) {
            User selectedUser;

            // Should always work, since you choose from list of users
            selectedUser = UserUtils.getUserByName(selectedUserName);

            if (showFromUser.isSelected()) {
                filteredStream = filteredStream.filter(expense ->
                        expense.getOriginalPayer().equals(selectedUser));
            } else if (showIncludingUser.isSelected()) {
                filteredStream = filteredStream.filter(expense ->
                        ExpenseUtils.getDebtorsOfExpense(expense.getId()).stream().
                                anyMatch(userExpense -> userExpense.getDebtor().equals(selectedUser)));
            }
        } else if (showFromUser.isSelected() || showIncludingUser.isSelected()) {
            showAll.setSelected(true);
        } else {
            showAll.setSelected(true);
        }
        return filteredStream;
    }


    /**
     * Refresh the userExpense table
      */
    public void refreshTableView() {
        expenseTable.setItems(FXCollections.observableList(
                    EventUtils.getExpensesOfEvent(ConfigUtils.getCurrentEvent().getId())));
        expenseTable.refresh();
        expenseTable.refresh();
    }

    private void addColorsToTagColorMenu() {
        tagColorMenu.getItems().clear();
        for (String colorKey : allColors) {
            String localizedColorName = LanguageController.getInstance().getString(colorKey);
            colorOtherLanguageToEnglish.put(localizedColorName, colorKey);
            MenuItem colorItem = new MenuItem(localizedColorName);
            colorItem.setOnAction(this::setColorForTag);
            tagColorMenu.getItems().add(colorItem);
        }
    }

    private String getRandomColor(Set<String> usedColors) {
        List<String> availableColors = this.allColors;
        availableColors.remove("Black");
        availableColors.remove("Gray"); //gray is ugly, black is last resort.
        List<String> selectionPool = new ArrayList<>();
        List<String> preferredColors = Arrays.asList("Blue", "Red", "Green", "Brown");
        availableColors.removeAll(usedColors);
        for (String color : preferredColors) {
            if (!usedColors.contains(color)) {
                selectionPool.add(color);
            }
        }
        if (selectionPool.isEmpty()) {
            for (String color : availableColors) {
                if (!usedColors.contains(color)) {
                    selectionPool.add(color);
                }
            }
        }

        if (availableColors.isEmpty()) {
            return "Black"; // if there are no colors left
        }

        Random rand = new Random();
        return selectionPool.get(rand.nextInt(selectionPool.size()));
    }

    /**
     * Restores the selection once returned to event overview.
     */
    public void restoreSelection() {
        if (expenseTable.getItems() != null) {
            if (lastSelectedIndex >= 0 && lastSelectedIndex < expenseTable.getItems().size()) {
                expenseTable.getSelectionModel().select(lastSelectedIndex);
                expenseTable.scrollTo(lastSelectedIndex);
                return;
            }
            expenseTable.getSelectionModel().clearSelection();
        }
    }

    /**
     * Show the expense as a string, if the expense is a result of Settling debts, show a custom string
     */
    public String expenseToString(Expense expense) {
        try {
            if ("Settle".equals(expense.getTag())) {
                List<UserExpense> ueList = ExpenseUtils.getDebtorsOfExpense(expense.getId());
                if (ueList.size() != 1) {
                    throw new Exception();
                }
                User debtor = ueList.get(0).getDebtor();
                return debtor.getUsername() + " " +
                        LanguageController.getInstance().getString("owes") +
                        " " + expense.getOriginalPayer().getUsername();
            }
        } catch (Exception e) {
            // show expense like normal
        }
        return expense.getOriginalPayer().getUsername() +
                " " + LanguageController.getInstance().getString("paid for") + " " +
                expense.getTitle();
    }

    private void showExpenseView() {
        lastSelectedIndex = expenseTable.getSelectionModel().getSelectedIndex();
        Expense selectedExpense = expenseTable.getSelectionModel().getSelectedItem();
        resetOnPageChange();
        webSocketUtils.unsubscribe();
        mainCtrl.showExpenseView(selectedExpense);
    }

    public void returnToStartScreen() {
        lastSelectedIndex = -1;
        resetOnPageChange();
        mainCtrl.showStartScreen();
    }

    public void addExpense() {
        lastSelectedIndex = -1;
        resetOnPageChange();
        mainCtrl.showExpenseAdd();
    }

    /**
     * Changes scene to the statistics scene
     */
    public void showStats() {
        lastSelectedIndex = -1;
        resetOnPageChange();
        System.out.println("Changed to Stats scene");
        mainCtrl.showStats();
    }

    public void invite() {
        lastSelectedIndex = -1;
        resetOnPageChange();
        mainCtrl.showInvitePage();
    }

    public void editParticipants() {
        lastSelectedIndex = -1;
        resetOnPageChange();
        mainCtrl.showAddEditParticipants();
    }

    public void settleDebt() {
        lastSelectedIndex = -1;
        this.mainCtrl.showSettleDebts();
    }

    /**
     *
     */
    public void resetOnPageChange() {
        tagMenu.setVisible(false);
        deleteExpenseButton.setVisible(false);
        tagTextField.setVisible(false);
        selectOrCreateTag.setVisible(false);
        submitTagButton.setVisible(false);
        tagColorMenu.setVisible(false);
        selectedUserName = null;
        expenseTable.setItems(null);
        eventUsersMenuButton.getItems().clear();
        tagFilterMenuButton.getItems().clear();
        showAll.setSelected(true);
        showIncludingUser.setDisable(true);
        showFromUser.setDisable(true);
        cancelEventNameChange();
        downloadJsonButton.setVisible(ConfigUtils.isAdmin());
    }

    /**
     * Deletes expense from tableview (and db via ExpenseUtils)
     */
    public void deleteExpense() {
        Expense expenseToRemove = ExpenseUtils.getExpenseById(selectedExpenseId);
        List<UserExpense> userExpenses = ExpenseUtils.getDebtorsOfExpense(selectedExpenseId);
        ExpenseUtils.removeExpense(selectedExpenseId);
        UndoUtils.addAction(new ExpenseAction(ExpenseAction.Type.DELETED, expenseToRemove, userExpenses));
        refreshTableView();
        applyFilters();
    }

    @FXML
    private void confirmDeleteExpense() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle(LanguageController.getInstance().getString("Confirm expense deletion"));
        confirmDialog.setHeaderText(LanguageController.getInstance().getString("Delete expense") + " " +
                ExpenseUtils.getExpenseById(selectedExpenseId).getTitle());
        confirmDialog.setContentText(LanguageController.getInstance().getString("confirm"));
        confirmDialog.getButtonTypes().clear();
        ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel =
                new ButtonType(LanguageController.getInstance().getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == buttonTypeOk) {
            deleteExpense();
        }
    }


    /**
     * Sets visibility of event name text field and all things related to name change to true so user can change the event name
     */
    public void showEventNameTextField() {
        editEventNameWrapper.setVisible(true);
        eventNameWrapper.setVisible(false);
        eventNameTextField.setEditable(true);
        eventNameTextField.requestFocus();
        eventNameTextField.setText(ConfigUtils.getCurrentEvent().getTitle());
    }

    /**
     * Sets visibility of all things related to event name change to false
     */
    public void cancelEventNameChange() {
        editEventNameWrapper.setVisible(false);
        eventNameWrapper.setVisible(true);
        eventNameTextField.setEditable(false);
    }

    /**
     * Applies the event name change once confirm button is pressed
     */
    public void confirmEventNameChange() {
        String newEventName = eventNameTextField.getText();
        if (newEventName.isEmpty() || newEventName == null) {
            mainCtrl.showAlert(LanguageController.getInstance().getString("The title cannot be empty"), Alert.AlertType.INFORMATION);
            return;
        }
        if (newEventName.equals(ConfigUtils.getCurrentEvent().getTitle())) {
            cancelEventNameChange();
        } else {
            ConfigUtils.getCurrentEvent().setTitle(newEventName);
            eventName.setText(newEventName + " \u270E");
            try {
                EventUtils.updateEvent(ConfigUtils.getCurrentEvent());
            } catch (FailedRequestException e) {
                mainCtrl.showAlert("Could not update event:\n\n" + e.getReason(), Alert.AlertType.ERROR);
            }
            cancelEventNameChange();
        }
    }

    /**
     * Updates the language of the scene
     */
    public void updateLanguage() {
        updateControllersLanguage();
        refreshEventOverview();
    }

    /**
     * updates the language of the labels
     */
    public void updateControllersLanguage() {
        selectOrCreateTag.setText(LanguageController.getInstance().getString("Select or create tag"));
        submitTagButton.setText(LanguageController.getInstance().getString("Submit tag"));
        expensesLabel.setText(LanguageController.getInstance().getString("expenses"));
        sendInviteButton.setText(LanguageController.getInstance().getString("Send invite"));
        statsButton.setText(LanguageController.getInstance().getString("Show stats"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
        tagColorMenu.setText(LanguageController.getInstance().getString("Select Color"));
        tagMenu.setText(LanguageController.getInstance().getString("Select tag"));
        addExpenseButton.setText(LanguageController.getInstance().getString("Add expense"));
        settleDebtButton.setText(LanguageController.getInstance().getString("Settle debt"));
        dateColumn.setText(LanguageController.getInstance().getString("date"));
        descriptionColumn.setText(LanguageController.getInstance().getString("description"));
        amountColumn.setText(LanguageController.getInstance().getString("amount"));
        totalAmountLabel.setText(LanguageController.getInstance().getString("Total amount"));
        showFromUser.setText(LanguageController.getInstance().getString("From user"));
        showIncludingUser.setText(LanguageController.getInstance().getString("Including user"));
        deleteExpenseButton.setText(LanguageController.getInstance().getString("Delete expense"));
        inviteCodeLabel.setText(LanguageController.getInstance().getString("Invite code"));
        tagColumn.setText(LanguageController.getInstance().getString("tag"));
        amountColumn.setText(LanguageController.getInstance().getString("amount"));
        showAll.setText(LanguageController.getInstance().getString("Show all"));
        eventUsersMenuButton.setText(LanguageController.getInstance().getString("Select User"));
        filtersLabel.setText(LanguageController.getInstance().getString("Filters"));
        downloadJsonButton.setText(LanguageController.getInstance().getString("Export json"));
    }

    private void refreshEventOverview() {
        boolean eventValid = true;
        try {
            EventUtils.getEventById(ConfigUtils.getCurrentEvent().getId());
        } catch (Exception e) {
            eventValid = false;
        }
        if (!eventValid) {
            return;
        }
        try {
            resetOnPageChange();
            init();
            restoreSelection();
            expenseTable.refresh();
        } catch (NullPointerException e) {
            System.out.println("No event is chosen yet.");
        }
    }

    public void downloadJson() {
        String jsonString = EventUtils.getJson(ConfigUtils.getCurrentEvent().getId());
        mainCtrl.downloadJson(ConfigUtils.getCurrentEvent(), jsonString);
    }
}

