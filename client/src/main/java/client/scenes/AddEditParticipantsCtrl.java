package client.scenes;

import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EventUtils;
import client.utils.UndoUtils;
import client.utils.UserUtils;
import commons.User;
import commons.exceptions.FailedRequestException;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AddEditParticipantsCtrl implements Initializable {

    private final MainCtrl mainCtrl;
    @FXML
    public Label event;
    @FXML
    public Label participantsLabel;
    @FXML
    public Label usersLabel;
    @FXML
    public TextField userNameInp;
    @FXML
    public Button editBtn;
    @FXML
    private Label eventTitle;
    @FXML
    private Label description;
    @FXML
    private ScrollPane participantsPane;
    @FXML
    private Button removeButton;
    @FXML
    private Button returnButton;

    private VBox participantsBox;
    private VBox usersBox;
    private List<User> participants;

    /**
     * The injected constructor
     * @param mainCtrl the main controller
     */
    @Inject
    public AddEditParticipantsCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(returnButton, "back");

        userNameInp.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    try {
                        User user = UserUtils.createOrGet(userNameInp.getText());
                        EventUtils.joinEvent(ConfigUtils.getCurrentEvent(), user);
                        userNameInp.setText("");
                        setParticipants();
                    } catch (FailedRequestException e) {
                        mainCtrl.showAlert(e.getReason(), Alert.AlertType.ERROR);
                    }
                }
            }
        });
        addShortcuts();
    }


    /**
     * Sets participants
     */
    public void setParticipants() {
        event.setText(ConfigUtils.getCurrentEvent().getTitle());

        participantsBox = new VBox();
        participantsBox.setSpacing(4);

        // Looping over all users and setting them to the participantsBox
        participants = EventUtils.getParticipants(ConfigUtils.getCurrentEvent().getId()).stream().toList();
        for (User usr : participants) {
            CheckBox c = new CheckBox(usr.getUsername());
            if (EventUtils.isUserInvolvedInExpenses(ConfigUtils.getCurrentEvent().getId(), usr.getId())) {
                c.setText(c.getText() + " (" + LanguageController.getInstance().getString("Part of expense") + ")");
            }
            participantsBox.getChildren().add(c);
        }
        participantsPane.setContent(participantsBox);
        Platform.runLater(this::updateLanguage);
    }

    /**
     * removes participant
     */
    public void removeParticipant() {
        List<User> removedParticipants = participantsBox.getChildren()
                .stream()
                .map(item -> (CheckBox) item)
                .filter(CheckBox::isSelected)
                .map(item -> {
                    try {
                        User user = UserUtils.getUserByName(item.getText());
                        if (EventUtils.isUserInvolvedInExpenses(ConfigUtils.getCurrentEvent().getId(), user.getId())) {
                            mainCtrl.showAlert("User " + item.getText() + " still has open expenses", Alert.AlertType.ERROR);
                            return null;
                        }
                        return user;
                    } catch (FailedRequestException e) {
                        mainCtrl.showAlert("Could not find user with username " + item.getText(), Alert.AlertType.ERROR);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        for (User user : removedParticipants) {
            try {
                EventUtils.removeUser(ConfigUtils.getCurrentEvent(), user);
            } catch (FailedRequestException e) {
                mainCtrl.showAlert("Could not remove " + user + " from event:\n\n" + e.getReason(), Alert.AlertType.ERROR);
            }
        }
        this.setParticipants();
        UndoUtils.resetHistory();
    }

    /**
     * adds participant
     */
    public void addParticipant() {
        List<User> newParticipants = usersBox.getChildren()
                .stream()
                .map(item -> (CheckBox) item)
                .filter(CheckBox::isSelected)
                .map(item -> {
                    try {
                        return UserUtils.getUserByName(item.getText());
                    } catch (FailedRequestException e) {
                        mainCtrl.showAlert("Could not find user with username " + item.getText(), Alert.AlertType.ERROR);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        for (User user : newParticipants) {
            try {
                EventUtils.joinEvent(ConfigUtils.getCurrentEvent(), user);
            } catch (FailedRequestException e) {
                mainCtrl.showAlert("Could not add " + user + " to event:\n\n" + e.getReason(), Alert.AlertType.ERROR);
            }
        }
        this.setParticipants();
    }

    /**
     * return to event overview
     */
    public void returnToEventOverview() {
        mainCtrl.showEventOverview();
    }

    /**
     * Called when edit participant button is pressed, only allows editing participant if exactly 1 participant is selected.
     */
    public void editParticipant() {
        List<CheckBox> checkBoxes = participantsBox.getChildren()
                .stream()
                .map(item -> (CheckBox) item).toList();
        if (checkBoxes.size() != participants.size()) {
            throw new RuntimeException("Size don't match, not supposed to happen");
        }
        int count = 0;
        for (CheckBox ch : checkBoxes) {
            if (ch.isSelected()) {
                count++;
            }
        }
        if (count > 1) {
            mainCtrl.showAlert("Can only edit 1 participant at once.", Alert.AlertType.WARNING);
            return;
        }
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                mainCtrl.showLoginScreen(participants.get(i).getId());
            }
        }
    }

    /**
     * Updates the language of the scene
     */
    public void updateLanguage() {
        System.out.println("Updating language of edit particpiants");
        description.setText(LanguageController.getInstance().getString("Add/edit participants of the event"));
        participantsLabel.setText(LanguageController.getInstance().getString("Participants of the event"));
        usersLabel.setText(LanguageController.getInstance().getString("Add users that are available on the server"));
        removeButton.setText(LanguageController.getInstance().getString("Remove from event"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = event.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        returnToEventOverview();
                        event.consume();
                    }
                });
            }
        });
    }
}
