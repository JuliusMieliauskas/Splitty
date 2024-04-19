package client.scenes;

import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.CurrencyUtils;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static client.LanguageTemplate.TEMPLATE;

/**
 * Controller for the settings scene.
 */
public class SettingsCtrl implements Initializable {

    private final MainCtrl mainCtrl;
    @FXML
    private Label currency;

    @FXML
    private Label signed_in_as;

    @FXML
    private Label settingsTitle;
    @FXML
    public Button adminLoginBtn;

    @FXML
    private Label languageTitleLabel;

    @FXML
    private Label language;

    @FXML
    private Button returnToOverviewButton;

    @FXML
    private Label username;

    @FXML
    private ChoiceBox<String> languageChoiceBox;

    @FXML
    private TextField languageTitle;

    @FXML
    private Label dictionaryLabel;

    @FXML
    private TextArea languageDictionary;

    @FXML
    private Button add_language_button;

    @FXML
    private Label new_language_input;

    @FXML
    private Button changeCredentialsButton;

    @FXML
    private ChoiceBox<String> currencyChoiceBox;

    /**
     * Constructor for the SettingsCtrl class.
     * @param mainCtrl The main controller of the application.
     */
    @Inject
    public SettingsCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     * @param url The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param resourceBundle The resources used to localize the root object, or null if the root object was not localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(changeCredentialsButton, "edit");
        mainCtrl.setButtonIcon(returnToOverviewButton, "back");

        List<String> availableLanguages = LanguageController.getAvailableLanguages();
        System.out.println(availableLanguages);
        languageChoiceBox.getItems().addAll(availableLanguages);
        languageChoiceBox.setValue(LanguageController.getInstance().capitalizedLocale());
        languageDictionary.setText(TEMPLATE);
        this.updateLanguage();

        currencyChoiceBox.getItems().addAll("USD", "EUR", "CHF");
        currencyChoiceBox.setValue(ConfigUtils.getCurrency());
        addShortcuts();

    }



    /**
     * Handles the action of the return to overview button.
     * @param event The event that triggered the method.
     */
    @FXML
    public void handleReturnToOverviewButtonAction(ActionEvent event) {
        System.out.println("returning to overview");
        System.out.println(event);
        mainCtrl.showStartScreen();
    }

    /**
     * Adds a new language to the app.
     */
    @FXML
    public void addLanguage(ActionEvent event) {
        System.out.println("adding language");
        try {
            LanguageController.addNewLanguage(languageTitle.getText(), languageDictionary.getText());
            languageChoiceBox.getItems().add(languageTitle.getText());
            languageDictionary.setText(TEMPLATE);
            this.mainCtrl.updateLanguage(new Locale(languageTitle.getText()));
            mainCtrl.showAlert("Successfully added a new language", AlertType.INFORMATION);
            languageChoiceBox.setValue(LanguageController.capitalizedLocale());
            languageTitle.clear();
            this.mainCtrl.updateAllLanguageChoiceBoxes();
        } catch (Exception e) {
            e.printStackTrace();
            mainCtrl.showAlert("Error in adding new language: " + e.getMessage(), AlertType.ERROR);
        }
    }

    /**
     * The username field now contains the email, since there is no more currenUser
     */
    public void loadUser() {
        if (ConfigUtils.getUserEmail() != null) {
            this.username.setText(ConfigUtils.getUserEmail());
        } else {
            this.username.setText("[no email added]");
        }
    }

    /**
     * Handles the change of language.
     * @param event The event that triggered the method.
     */
    @FXML
    public void handleLanguageChange(ActionEvent event) {
        mainCtrl.updateLanguage(new Locale(languageChoiceBox.getValue()));
    }

    /**
     * opens up credentials popup
     */
    public void changeCredentials() {
        mainCtrl.showLoginScreen();
    }

    /**
     * Updates the selected currency also the exchange rate
     */
    public void updateCurrency() {
        if (ConfigUtils.getServerUrl() == null) {
            return;
        }
        System.out.println("updating currency");
        System.out.println(currencyChoiceBox.getValue());
        try {
            Double exchangeRate = CurrencyUtils.getExchangeRate(currencyChoiceBox.getValue());
            this.currencyChoiceBox.setValue(currencyChoiceBox.getValue());
            ConfigUtils.setCurrency(currencyChoiceBox.getValue(), exchangeRate);
        } catch (Exception e) {
            mainCtrl.showAlert("Error in updating currency: " + e.getMessage(), AlertType.ERROR);
        }
    }

    public void openAdminLogin() {
        mainCtrl.showAdminLoginScreen();
    }


    /**
     * Updates the language of the settings page.
     */
    public void updateLanguage() {
        System.out.println("updating language of Settings page");
        signed_in_as.setText(LanguageController.getInstance().getString("Signed in as"));
        settingsTitle.setText(LanguageController.getInstance().getString("settings"));
        language.setText(LanguageController.getInstance().getString("language"));
        currency.setText(LanguageController.getInstance().getString("currency"));
        languageTitleLabel.setText(LanguageController.getInstance().getString("title"));
        returnToOverviewButton.setText(LanguageController.getInstance().getString("return"));
        new_language_input.setText(LanguageController.getInstance().getString("New language input"));
        add_language_button.setText(LanguageController.getInstance().getString("Add language"));
        dictionaryLabel.setText(LanguageController.getInstance().getString("dictionary"));
        changeCredentialsButton.setText(LanguageController.getInstance().getString("Change credentials"));
        adminLoginBtn.setText(LanguageController.getInstance().getString("Admin Login"));
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = settingsTitle.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        ActionEvent e = new ActionEvent();
                        handleReturnToOverviewButtonAction(e);
                        event.consume();
                    }
                    if (event.getCode() == KeyCode.A) {
                        openAdminLogin();
                        event.consume();
                    }
                    if (event.getCode() == KeyCode.E) {
                        changeCredentials();
                        event.consume();
                    }
                });
            }
        });
        languageDictionary.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                add_language_button.requestFocus();
                e.consume();
            }
        });
    }
}