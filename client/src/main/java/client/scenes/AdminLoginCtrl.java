package client.scenes;

import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.UserUtils;
import com.google.inject.Inject;
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

public class AdminLoginCtrl implements Initializable {
    private final MainCtrl mainCtrl;

    @FXML
    private TextField passwordField;
    @FXML
    private Button returnButton;
    @FXML
    private Button continueButton;
    @FXML
    private Label enterPassword;

    @Inject
    public AdminLoginCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(this::updateLanguage);
        addShortcuts();
    }

    /**
     * Try to enter the password, checks if it's correct
     */
    public void enter() {
        String inp = passwordField.getText();
        if (inp.isEmpty()) {
            return;
        }

        boolean correct = UserUtils.verifyAdminPassword(inp);

        if (!correct) {
            mainCtrl.showAlert("Incorrect password", Alert.AlertType.INFORMATION);
        }
        ConfigUtils.setAdmin(correct);
        if (correct) {
            mainCtrl.showStartScreen();
        }
    }

    public void back() {
        mainCtrl.showSettings();
    }

    /**
     * updates the language of the scene
     */
    public void updateLanguage() {
        System.out.println("update language of admin");
        enterPassword.setText(LanguageController.getInstance().getString("Enter password"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
        continueButton.setText(LanguageController.getInstance().getString("continue"));
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = returnButton.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        back();
                        event.consume();
                    }
                });
            }
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                enter();
                event.consume();
            }
        });
    }

}
