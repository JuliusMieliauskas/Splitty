package client.scenes;

import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EmailUtils;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.util.ResourceBundle;

public class InvitePageCtrl implements Initializable {

    private final MainCtrl mainCtrl;

    @FXML
    private Label eventName;

    @FXML
    private Label eventCode;

    @FXML
    private TextArea textArea;

    @FXML
    private Label inviteDescription;

    @FXML
    private Label emailingDescription;

    @FXML
    private Button sendInvitesButton;

    @FXML
    private Button returnButton;

    @Inject
    public InvitePageCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        Platform.runLater(this::updateLanguage);
    }

    /**
     * sets the event of this page
     */
    public void init() {
        sendInvitesButton.setDisable(ConfigUtils.getUserEmail() == null ||
                ConfigUtils.getEmailPassword() == null);
        String eventCodeAcc = ConfigUtils.getCurrentEvent().getInviteCode();
        String eventNameAcc = ConfigUtils.getCurrentEvent().getTitle();
        eventCode.setText(eventCodeAcc);
        eventName.setText(eventNameAcc);
        textArea.clear();
    }

    /**
     * initialize the page
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(returnButton, "back");
        addShortcuts();
    }

    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = eventName.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        back();
                        event.consume();
                    }
                });
            }
        });
    }

    /**
     * button to go back to EventOverview
     */
    public void back() {
        textArea.clear();
        mainCtrl.showEventOverview();
    }

    /**
     * Send email button
     * @throws Exception
     */
    public void sendInvites() throws Exception {
        try {
            EmailUtils.sendInvites(
                    ConfigUtils.getUserEmail(),
                    ConfigUtils.getEmailPassword(),
                    textArea.getText(),
                    ConfigUtils.getCurrentEvent());
            textArea.clear();
            mainCtrl.showEventOverview();
        } catch (Exception e) {
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Email did not send"),
                    Alert.AlertType.INFORMATION);
        }
    }

    /**
     * updates the language of the scene
     */
    public void updateLanguage() {
        inviteDescription.setText(LanguageController.getInstance().getString("Give people the following invite code"));
        emailingDescription.setText(
                LanguageController.getInstance().getString("Invite the following people by email") + " (" +
                LanguageController.getInstance().getString("One address per line") + ")\n" +
                        LanguageController.getInstance().getString("In this format"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
        sendInvitesButton.setText(LanguageController.getInstance().getString("Send invites"));
    }
}
