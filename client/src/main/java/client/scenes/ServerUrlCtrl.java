package client.scenes;

import client.LanguageController;
import client.utils.ConfigUtils;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerUrlCtrl implements Initializable {
    @FXML
    private Label serverUnavailError;
    @FXML
    private TextField url;
    @FXML
    private Label addServerUrlLabel;
    private final MainCtrl mainCtrl;

    @Inject
    public ServerUrlCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(this::updateLanguage);
    }

    /**
     * Called when url is inputted. If url is valid and reachable the client continues
     */
    public void inputUrl() {
        serverUnavailError.setVisible(false);
        String server = url.getText();
        if (ConfigUtils.isServerAvailable(server)) {
            ConfigUtils.setServerUrl(server);
            mainCtrl.startUp();
        } else {
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Server Not Available"), Alert.AlertType.ERROR);
        }
    }

    /**
     * upates the language of the scene
     */
    public void updateLanguage() {
        serverUnavailError.setText(LanguageController.getInstance().getString("Server Not Available"));
        addServerUrlLabel.setText(LanguageController.getInstance().getString("Add Server Url"));
    }
}
