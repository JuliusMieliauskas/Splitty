package client.scenes;

import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EmailUtils;
import client.utils.UserUtils;
import com.google.inject.Inject;
import commons.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginCtrl implements Initializable {
    private final MainCtrl mainCtrl;
    @FXML
    private Label emailMsg;
    @FXML
    private TextField userName;
    @FXML
    private Label ibanMsg;
    @FXML
    private TextField ibanInput;
    @FXML
    private Button testEmailBtn;
    @FXML
    private Label localMsg;
    @FXML
    private Button returnbtn;
    @FXML
    private TextField emailInput;
    Long otherUser;
    @FXML
    private Label passMsg;
    @FXML
    private TextField passInput;
    @FXML
    private Label loginInstruction;

    @Inject
    public LoginCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(returnbtn, "back");
        Platform.runLater(this::updateLanguage);
        addShortcuts();
    }


    /**
     * Init for current user
     */
    public void init() {
        otherUser = -1L;
        if (ConfigUtils.getUserEmail() != null) {
            emailInput.setText(ConfigUtils.getUserEmail());
        } else {
            emailInput.setText("");
        }
        if (ConfigUtils.getEmailPassword() != null) {
            passInput.setText(ConfigUtils.getEmailPassword());
        } else {
            passInput.setText("");
        }
        localMsg.setVisible(false);
        ibanMsg.setVisible(false);
        ibanInput.setVisible(false);
        userName.setVisible(false);
        testEmailBtn.setVisible(true);
        passInput.setVisible(true);
        passMsg.setVisible(true);
    }

    /**
     * Add creds to other user
     */
    public void init(Long userId) {
        otherUser = userId;
        if (ConfigUtils.getEmail(userId) != null) {
            emailInput.setText(ConfigUtils.getEmail(userId));
        } else {
            emailInput.setText("");
        }
        if (ConfigUtils.getIban(userId) != null) {
            ibanInput.setText(ConfigUtils.getIban(userId));
        } else {
            ibanInput.setText("");
        }

        userName.setText(UserUtils.getUserById(userId.intValue()).getUsername());
        localMsg.setVisible(true);
        ibanMsg.setVisible(true);
        ibanInput.setVisible(true);
        userName.setVisible(true);
        testEmailBtn.setVisible(false);
        passInput.setVisible(false);
        passMsg.setVisible(false);
    }

    /**
     * Enter the user credentials
     */
    public void enterInfo() {
        if (otherUser != -1) {
            if (userName.getText().isEmpty()) {
                mainCtrl.showAlert("userName cannot be empty", Alert.AlertType.ERROR);
                return;
            }
            User user = UserUtils.getUserById(otherUser.intValue());
            if (!user.getUsername().equals(userName.getText())) {
                System.out.println("Changing userName");
                UserUtils.renameUser(user.getId(), userName.getText());
            }

            ConfigUtils.setEmail(otherUser, emailInput.getText().isEmpty() ? null : emailInput.getText());
            ConfigUtils.setIban(otherUser, ibanInput.getText().isEmpty() ? null : ibanInput.getText());
            mainCtrl.showEventOverview();
        } else {
            ConfigUtils.setEmailPassword(passInput.getText().isEmpty() ? null : passInput.getText());
            ConfigUtils.setUserEmail(emailInput.getText().isEmpty() ? null : emailInput.getText());
            ConfigUtils.setAdmin(false);
            mainCtrl.showStartScreen();
        }
    }

    /**
     * Button that sends default email as a test
     */
    public void testEmail() {
        ConfigUtils.setEmailPassword(passInput.getText().isEmpty() ? null : passInput.getText());
        ConfigUtils.setUserEmail(emailInput.getText().isEmpty() ? null : emailInput.getText());
        try {
            EmailUtils.sendDefault(ConfigUtils.getUserEmail(), ConfigUtils.getEmailPassword());
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
        loginInstruction.setText(LanguageController.getInstance().getString("Please enter your information"));
        returnbtn.setText(LanguageController.getInstance().getString("return"));
        localMsg.setText(LanguageController.getInstance().getString("Local information"));
        returnbtn.setText(LanguageController.getInstance().getString("return"));
        emailMsg.setText(LanguageController.getInstance().getString("Email"));
    }

    /**
     * Cancel changing stuff
     */
    public void cancel() {
        if (otherUser == -1) {
            mainCtrl.showSettings();
        } else {
            mainCtrl.showEventOverview();
        }
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = ibanMsg.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancel();
                        event.consume();
                    }
                });
            }
        });
    }

}
