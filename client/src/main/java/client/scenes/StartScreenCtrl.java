package client.scenes;

import client.LanguageController;
import client.LanguageChoiceBox;
import client.utils.ConfigUtils;
import client.utils.EventUtils;
import client.utils.UndoUtils;
import client.utils.ExpenseUtils;
import client.utils.UserUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import commons.Event;

import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import jakarta.ws.rs.WebApplicationException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class StartScreenCtrl implements Initializable {
    private final MainCtrl mainCtrl;

    private ObservableList<Event> myEvents;
    private SortedList<Event> myEventsSortAlphabet;
    private SortedList<Event> myEventsSortNewest;
    private SortedList<Event> myEventsSortLatest;

    @FXML
    private TextField createEvent;

    @FXML
    private TextField joinEvent;

    @FXML
    private ListView<Event> activeEvents;

    @FXML
    private Button deleteButton;

    @FXML
    private Button joinButton;

    @FXML
    private Button createButton;

    @FXML
    private Label createNewEventLabel;

    @FXML
    private Label joinEventLabel;

    @FXML
    private Label allYourEventsLabel;
    @FXML
    private VBox languageChoiceBoxContainer;

    public LanguageChoiceBox languageChoiceBox;

    @FXML
    private MenuButton sortMenu;

    @FXML
    private MenuItem sortA;

    @FXML
    private MenuItem sortN;

    @FXML
    private MenuItem sortL;
    @FXML
    private Button settingsBtn;

    @FXML
    private Button importJsonButton;

    private String selectedSorting = "Newest Event";


    @Inject
    public StartScreenCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.languageChoiceBox = new LanguageChoiceBox(mainCtrl);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(settingsBtn, "settings");
        mainCtrl.setButtonIcon(createButton, "create");
        mainCtrl.setButtonIcon(joinButton, "join");
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (ConfigUtils.isAdmin()) {
                        refreshAdmin();
                    } else {
                        refresh();
                    }
                });
            }
        }, 0, 1000);


        if (ConfigUtils.isAdmin()) {
            refreshAdmin();
        } else {
            refresh();
        }
        //TODO Else statement with some error
        activeEvents.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });

        //go to event on double click
        doubleClick();

        activeEvents.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (activeEvents.getSelectionModel().getSelectedItem() != null) {
                    ConfigUtils.setCurrentEvent(activeEvents.getSelectionModel().getSelectedItem());
                    mainCtrl.showEventOverview();
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.DELETE) {
                if (activeEvents.getSelectionModel().getSelectedItem() != null) {
                    delete();
                    event.consume();
                }
            }
        });

        // make delete option for when an event is selected
        activeEvents.getSelectionModel().selectedItemProperty().addListener((observableValue, event, t1) -> {
            deleteButton.setVisible(true);
        });
        Platform.runLater(this::updateLanguage);
        languageChoiceBoxContainer.getChildren().add(languageChoiceBox);
        addShortcuts();
    }

    /**
     * double click function
     */
    private void doubleClick() {
        activeEvents.setOnMouseClicked((MouseEvent click) -> {
            if (click.getClickCount() == 2 &&
                    activeEvents.getSelectionModel().getSelectedItem() != null) {
                ConfigUtils.setCurrentEvent(activeEvents.getSelectionModel().getSelectedItem());
                UndoUtils.resetHistory();
                mainCtrl.showEventOverview();
            }
        });
    }




    /**
     * the create button functionality
     */
    public void create() {
        try {
            String title = createEvent.getText();
            if (title == null) {
                throw new WebApplicationException();
            }
            Event event = new Event(title);
            event = EventUtils.createEvent(event);
            clearFields();
            ConfigUtils.setCurrentEvent(event);
            ConfigUtils.addEvent(event.getId());
            mainCtrl.showEventOverview();
        } catch (FailedRequestException e) {
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("The title cannot be empty"),
                    Alert.AlertType.INFORMATION);
        }
    }

    /**
     * the join button functionality
     */
    public void join() {
        try {
            Event event = EventUtils.getEventByCode(joinEvent.getText());
            ConfigUtils.addEvent(event.getId());
            ConfigUtils.setCurrentEvent(event);
            mainCtrl.showEventOverview();
        } catch (FailedRequestException e) {
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Incorrect invite code"),
                    Alert.AlertType.ERROR);
        }
        clearFields();
    }

    /**
     * delete button to leave a selected event
     */
    public void delete() {
        Event eventToDelete = activeEvents.getSelectionModel().getSelectedItem();

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        if (!ConfigUtils.isAdmin()) {
            confirmDialog.setHeaderText(LanguageController.getInstance().getString("confirm event removal from list"));
        } else {
            confirmDialog.setHeaderText(LanguageController.getInstance().getString("confirm event deletion from database"));
        }
        confirmDialog.setTitle(LanguageController.getInstance().getString("Delete event") + " " + eventToDelete.getTitle());
        confirmDialog.setContentText(LanguageController.getInstance().getString("confirm"));
        confirmDialog.getButtonTypes().clear();
        ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel =
                new ButtonType(LanguageController.getInstance().getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == buttonTypeOk) {
            ConfigUtils.removeEvent(eventToDelete.getId());
            if (ConfigUtils.isAdmin()) {
                EventUtils.deleteEvent(eventToDelete);
            }
        }

        if (ConfigUtils.isAdmin()) {
            refreshAdmin();

        } else {
            refresh();
        }
    }

    /**
     * refreshes the events in the listview
     */
    public void refresh() {
        List<Long> eventIdsToRemove = new ArrayList<>();
        List<Event> events = ConfigUtils.getEvents().stream().map(eventId -> {
            try {
                return EventUtils.getEventById(eventId);
            } catch (FailedRequestException e) {
                eventIdsToRemove.add(eventId);
                return null;
            }
        }).filter(Objects::nonNull).toList();
        eventIdsToRemove.forEach(ConfigUtils::removeEvent);
        myEvents = FXCollections.observableList(events);
        sort();
        activeEvents.setItems(myEvents);
        activeEvents.setPlaceholder(new Label(LanguageController.getInstance().getString("No events available")));

        importJsonButton.setVisible(false);
    }

    /**
     * refreshes to see all events for admin
     */
    public void refreshAdmin() {
        var events = EventUtils.getEvents();
        myEvents = FXCollections.observableList(events);
        sort();
        activeEvents.setItems(myEvents);

        importJsonButton.setVisible(true);
    }

    private void clearFields() {
        createEvent.clear();
        joinEvent.clear();
    }

    /**
     * Import a json file of an event, can only be used when admin
     */
    public void importJson() {
        ConfigUtils.setCurrentEvent(null);

        String json = mainCtrl.getJsonFromFile();
        User[] jsonUsers;
        Event jsonEvent;
        Expense[] jsonExpenses;
        UserExpense[] jsonUserExpenses;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // for dateTime support :)))
            JsonNode jsonNode = objectMapper.readTree(json);

            jsonUsers = objectMapper.treeToValue(jsonNode.path("users"), User[].class);
            jsonEvent = objectMapper.treeToValue(jsonNode.path("event"), Event.class);
            jsonExpenses = objectMapper.treeToValue(jsonNode.path("expenses"), Expense[].class);
            jsonUserExpenses = objectMapper.treeToValue(jsonNode.path("userExpenses"), UserExpense[].class);

            //save all those objects backup, get the invite code for the success message
            String code = saveImportedObjects(jsonUsers, jsonEvent, jsonExpenses, jsonUserExpenses);

            mainCtrl.showAlert("The imported event was successfully saved! Join the event with code: " + code, Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            mainCtrl.showAlert("Could not save the imported event", Alert.AlertType.ERROR);
            return;
        }

        refreshAdmin();
    }

    /**
     * Save the imported event to the database (note that it overwrites the id's of the imported objects)
     *
     * @param jsonUsers        list of users
     * @param jsonEvent        event
     * @param jsonExpenses     list of expenses
     * @param jsonUserExpenses list of user expenses
     */
    public String saveImportedObjects(User[] jsonUsers, Event jsonEvent, Expense[] jsonExpenses, UserExpense[] jsonUserExpenses) {
        //So, the repo's do not allow .save(obj) if that obj already has an id (if the id is @generated)
        //So, the id's get deleted and the id's get updated for connected expenses, users, user expenses, events
        HashMap<Long, Long> userIdChanges = new HashMap<>();
        HashMap<Long, Long> expenseIdChanges = new HashMap<>();

        for (int i = 0; i < jsonUsers.length; i++) {
            //save user and keep it stored so related stuff can be updated
            Long firstId = jsonUsers[i].getId();
            jsonUsers[i].setId(null);
            jsonUsers[i] = UserUtils.createUser(jsonUsers[i]);
            userIdChanges.put(firstId, jsonUsers[i].getId());
        }
        //create the event and join all users
        jsonEvent = createEventAndLetUsersJoin(jsonUsers, jsonEvent, userIdChanges);

        for (Expense e : jsonExpenses) {
            //update id from related objects
            e.getOriginalPayer().setId(userIdChanges.get(e.getOriginalPayer().getId()));
            e.setEvent(jsonEvent);

            //save expense and keep it stored so related stuff can be updated
            Long firstId = e.getId();
            e.setId(null);
            e = ExpenseUtils.createExpense(e);
            expenseIdChanges.put(firstId, e.getId());
        }
        for (UserExpense ue : jsonUserExpenses) {
            //update id from related objects
            ue.getDebtor().setId(userIdChanges.get(ue.getDebtor().getId()));
            ue.getExpense().setId(expenseIdChanges.get(ue.getExpense().getId()));
            ue.getExpense().getOriginalPayer().setId(userIdChanges.get(ue.getExpense().getOriginalPayer().getId()));
            ue.getExpense().setEvent(jsonEvent);

            //skip all the redundant user expenses where the payer == the debtor
            if (ue.getDebtor().equals(ue.getExpense().getOriginalPayer())) {
                continue;
            }

            //save user expense
            ue.setId(null);
            ExpenseUtils.addUserToExpense(ue.getExpense().getId(), ue);
        }
        return jsonEvent.getInviteCode();
    }

    /**
     * Creates event and lets all users in a list join an event
     *
     * @param us list of users
     * @param e  the event
     */
    public Event createEventAndLetUsersJoin(User[] us, Event e, HashMap<Long, Long> userIdChanges) {
        //save event and keep it stored so related stuff can be updated
        e.setId(null);
        String inviteCodeTempSave = e.getInviteCode();

        //create the event
        e = EventUtils.createEvent(e);

        //join every user to this event
        for (User u : us) {
            EventUtils.joinEvent(e, u);
        }

        //I try to keep the save invite code when importing the event
        String newInviteCode = e.getInviteCode();
        try {
            e.setInviteCode(inviteCodeTempSave);
            EventUtils.updateEvent(e);
        } catch (Exception ex) {
            //but when it is taken just have the new invite code
            e.setInviteCode(newInviteCode);
            System.out.println("The invite code of the imported event was already taken, the new invite code is: " + newInviteCode);
        }
        return e;
    }

    /**
     * updates the language of the scene
     */
    public void updateLanguage() {
        createNewEventLabel.setText(LanguageController.getInstance().getString("Create new event"));
        joinEventLabel.setText(LanguageController.getInstance().getString("Join event"));
        allYourEventsLabel.setText(LanguageController.getInstance().getString("All your events"));
        createButton.setText(LanguageController.getInstance().getString("create"));
        joinButton.setText(LanguageController.getInstance().getString("join"));
        deleteButton.setText(LanguageController.getInstance().getString("delete"));
        settingsBtn.setText(LanguageController.getInstance().getString("settings"));
        sortA.setText(LanguageController.getInstance().getString("Alphabetically"));
        sortN.setText(LanguageController.getInstance().getString("Newest Event"));
        sortL.setText(LanguageController.getInstance().getString("Latest Activity"));
        importJsonButton.setText(LanguageController.getInstance().getString("Import json"));
        sortMenu.setText(LanguageController.getInstance().getString(selectedSorting));
    }

    /**
     * Menu item to sort events alphabetically
     */
    public void sortAlphabetically() {
        if (ConfigUtils.isAdmin()) {
            refreshAdmin();
        } else {
            refresh();
        }
        selectedSorting = "Alphabetically";
        activeEvents.setItems(myEventsSortAlphabet);
        sortMenu.setText(LanguageController.getInstance().getString("Alphabetically"));
    }

    /**
     * Menu item to sort events by newest creation date
     */
    public void sortNewest() {
        if (ConfigUtils.isAdmin()) {
            refreshAdmin();
        } else {
            refresh();
        }
        selectedSorting = "Newest Event";
        activeEvents.setItems(myEventsSortNewest);
        sortMenu.setText(LanguageController.getInstance().getString("Newest Event"));
    }

    /**
     * Menu item to sort events by latest activity
     */
    public void sortLatest() {
        if (ConfigUtils.isAdmin()) {
            refreshAdmin();
        } else {
            refresh();
        }
        selectedSorting = "Latest Activity";
        activeEvents.setItems(myEventsSortLatest);
        sortMenu.setText(LanguageController.getInstance().getString("Latest Activity"));
    }

    /**
     * sorts new lists
     */
    public void sort() {
        var temp = myEvents;
        myEventsSortAlphabet = new SortedList<>(temp, new Alphabetically());
        myEventsSortNewest = new SortedList<>(temp, new Newest());
        myEventsSortLatest = new SortedList<>(temp, new Latest());
    }

    public void toSettings() {
        mainCtrl.showSettings();
    }

    private final static class Alphabetically implements Comparator<Event> {
        @Override
        public int compare(Event e1, Event e2) {
            String t1 = e1.getTitle();
            String t2 = e2.getTitle();
            int comp = t1.compareTo(t2);
            if (comp > 0) {
                return 1;
            } else if (comp < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private final static class Newest implements Comparator<Event> {
        @Override
        public int compare(Event e1, Event e2) {
            LocalDateTime t1 = e1.getCreationDate();
            LocalDateTime t2 = e2.getCreationDate();
            int comp = t1.compareTo(t2);
            if (comp < 0) {
                return 1;
            } else if (comp > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private final static class Latest implements Comparator<Event> {
        @Override
        public int compare(Event e1, Event e2) {
            LocalDateTime t1 = e1.getLastActivity();
            LocalDateTime t2 = e2.getLastActivity();
            int comp = t1.compareTo(t2);
            if (comp < 0) {
                return 1;
            } else if (comp > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = createButton.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    System.out.println("Key pressed: " + event.getCode());
                    if (event.getCode() == KeyCode.S) {
                        toSettings();
                        event.consume();
                    }
                });
            }
        });
        languageChoiceBoxContainer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                languageChoiceBox.show();
            }
        });
        sortNewest();

        createEvent.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                create();
                event.consume();
            }
        });
        joinEvent.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                join();
                event.consume();
            }
        });
    }
}