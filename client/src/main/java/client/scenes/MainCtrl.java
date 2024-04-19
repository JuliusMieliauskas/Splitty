package client.scenes;

import client.LanguageChoiceBox;
import client.LanguageController;
import client.utils.ConfigUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import commons.Event;
import commons.Expense;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainCtrl {
    private Stage primaryStage;

    private Scene addEditExpense;
    private AddEditExpenseCtrl addEditExpenseCtrl;

    private Scene addEditParticipants;
    private AddEditParticipantsCtrl addEditParticipantsCtrl;

    private Scene eventOverview;
    private EventOverviewCtrl eventOverviewCtrl;

    private Scene expenseView;
    private ExpenseViewCtrl expenseViewCtrl;

    private Scene invitePage;
    private InvitePageCtrl invitePageCtrl;

    private Scene settings;
    private SettingsCtrl settingsCtrl;

    private Scene settleDebts;
    private SettleDebtsCtrl settleDebtsCtrl;

    private Scene startScreen;
    private StartScreenCtrl startScreenCtrl;

    private Scene stats;
    private StatsCtrl statsCtrl;


    private Scene loginScreen;
    private LoginCtrl loginCtrl;

    private Scene serverUrlScreen;
    private ServerUrlCtrl serverUrlCtrl;

    private Scene adminLoginScreen;
    private AdminLoginCtrl adminLoginCtrl;
    private static MainCtrl instance;

    private List<LanguageChoiceBox> languageChoiceBoxes = new ArrayList<>();

    /**
     * Load config file and make sure the server is available
     */
    public MainCtrl() {
        instance = this;
        ConfigUtils.tryLoadConfig();
        if (ConfigUtils.getServerUrl() != null && !ConfigUtils.isServerAvailable(ConfigUtils.getServerUrl())) {
            showAlert("The server " + ConfigUtils.getServerUrl() + " is not available", Alert.AlertType.ERROR);
            System.exit(1);
        }
        Thread.setDefaultUncaughtExceptionHandler(new MyErrorHandler());
    }

    private final class MyErrorHandler implements Thread.UncaughtExceptionHandler {
        private boolean showingMessage = false;
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (showingMessage) {
                return;
            }
            showingMessage = true;
            if ("java.net.ConnectException: Connection refused: connect".equals(e.getMessage())) {
                showAlert("The server " + ConfigUtils.getServerUrl() + " is not available", Alert.AlertType.ERROR);
                System.exit(1);
            }
            showAlert("Error:\n" + e.getMessage(), Alert.AlertType.ERROR);
            showingMessage = false;
        }
    }

    /**
     * Initializes the stages for the client application
     * @param primaryStage          Primary Stage
     * @param addEditExpense        add / edit Expenses
     * @param addEditParticipants   add / edit participants
     * @param eventOverview         overview of event
     * @param expenseView           overview of expense
     * @param invitePage            invite page
     * @param settings              settings page
     * @param settleDebts           settle debts page
     * @param startScreen           start screen
     * @param stats                 statistics
     * @param loginScreen           Screen where user can enter their information (name, mail, etc)
     * @param adminLogin            Login screeen for the admin
     */
    public void initialize(Stage primaryStage,
                           Pair<AddEditExpenseCtrl, Parent> addEditExpense,
                           Pair<AddEditParticipantsCtrl, Parent> addEditParticipants,
                           Pair<EventOverviewCtrl, Parent> eventOverview,
                           Pair<ExpenseViewCtrl, Parent> expenseView,
                           Pair<InvitePageCtrl, Parent> invitePage,
                           Pair<SettingsCtrl, Parent> settings,
                           Pair<SettleDebtsCtrl, Parent> settleDebts,
                           Pair<StartScreenCtrl, Parent> startScreen,
                           Pair<StatsCtrl, Parent> stats,
                           Pair<LoginCtrl, Parent> loginScreen,
                           Pair<AdminLoginCtrl, Parent> adminLogin,
                           Pair<ServerUrlCtrl, Parent> serverUrlInp
                           ) {
        this.primaryStage = primaryStage;

        this.serverUrlCtrl = serverUrlInp.getKey();
        this.serverUrlScreen = new Scene(serverUrlInp.getValue());

        this.loginCtrl = loginScreen.getKey();
        this.loginScreen = new Scene(loginScreen.getValue());

        this.adminLoginCtrl = adminLogin.getKey();
        this.adminLoginScreen = new Scene(adminLogin.getValue());

        this.addEditExpenseCtrl = addEditExpense.getKey();
        this.addEditExpense = new Scene(addEditExpense.getValue());

        this.addEditParticipantsCtrl = addEditParticipants.getKey();
        this.addEditParticipants = new Scene(addEditParticipants.getValue());

        this.eventOverviewCtrl = eventOverview.getKey();
        this.eventOverview = new Scene(eventOverview.getValue());

        this.expenseViewCtrl = expenseView.getKey();
        this.expenseView = new Scene(expenseView.getValue());

        this.invitePageCtrl = invitePage.getKey();
        this.invitePage = new Scene(invitePage.getValue());

        this.settingsCtrl = settings.getKey();
        this.settings = new Scene(settings.getValue());

        this.settleDebtsCtrl = settleDebts.getKey();
        this.settleDebts = new Scene(settleDebts.getValue());

        this.startScreenCtrl = startScreen.getKey();
        this.startScreen = new Scene(startScreen.getValue());

        this.statsCtrl = stats.getKey();
        this.stats = new Scene(stats.getValue());

        startUp();
        //showStartScreen();
        //showSettings();
        primaryStage.show();
    }

    /**
     * Show the first screen of the application.
     * Depending on the config file, the user may need to add serverUrl and credentials first
     */
    public void startUp() {
        if (ConfigUtils.getServerUrl() == null) {
            showServerUrlInpScreen();
        } else {
            this.eventOverviewCtrl.initWebSocket(ConfigUtils.getServerUrl());
            showStartScreen();
        }
    }

    /**
     * shows the login screen, if necessary with the previous details
     */
    public void showLoginScreen() {
        loginCtrl.init();
        primaryStage.setTitle("");
        primaryStage.setScene(loginScreen);
    }

    /**
     * shows the login screen for another user, in which their mail can be entered
     */
    public void showLoginScreen(Long userid) {
        loginCtrl.init(userid);
        primaryStage.setTitle("");
        primaryStage.setScene(loginScreen);
    }

    public void showServerUrlInpScreen() {
        primaryStage.setTitle("");
        primaryStage.setScene(serverUrlScreen);
    }

    public void showAdminLoginScreen() {
        primaryStage.setTitle("");
        primaryStage.setScene(adminLoginScreen);
    }

    /**
     * show the settings page
     */
    public void showSettings() {
        settingsCtrl.loadUser();
        primaryStage.setTitle("Settings");
        primaryStage.setScene(settings);
    }

    /**
     * Show the statistics scene
     */
    public void showStats() {
        statsCtrl.setEvent(ConfigUtils.getCurrentEvent());
        primaryStage.setTitle("Stats Scene");
        primaryStage.setScene(stats);
        primaryStage.show();
    }

    /**
     * Switches scene to Event Overview
     */
    public void showEventOverview() {
        eventOverviewCtrl.init();
        eventOverviewCtrl.restoreSelection();
        eventOverviewCtrl.languageChoiceBox.updateLanguage();
        primaryStage.setTitle("");
        primaryStage.setScene(eventOverview);
    }

    /**
     * Shows expense overview screen
     * @param selectedExpense the expense that will be shown
     */
    public void showExpenseView(Expense selectedExpense) {
        expenseViewCtrl.init(selectedExpense);
        expenseViewCtrl.updateLanguage();
        primaryStage.setTitle("");
        primaryStage.setScene(expenseView);
    }


    /**
     * Shows the settle debts page
     */
    public void showSettleDebts() {
        settleDebtsCtrl.refreshTransactions();
        primaryStage.setTitle("debts!");
        primaryStage.setScene(settleDebts);
    }

    /**
     * shows the add/edit expense screen
     */
    public void showExpenseAdd() {
        addEditExpenseCtrl.init(null);
        primaryStage.setScene(addEditExpense);
    }
    public void showExpenseAdd(Expense expense) {
        addEditExpenseCtrl.init(expense);
        primaryStage.setScene(addEditExpense);
    }


    public void showAddEditParticipants() {
        addEditParticipantsCtrl.setParticipants();
        primaryStage.setScene(addEditParticipants);
    }

    /**
     * Show the start screen
     */
    public void showStartScreen() {
        if (ConfigUtils.isAdmin()) {
            startScreenCtrl.refreshAdmin();
        } else {
            startScreenCtrl.refresh();
        }
        primaryStage.setTitle("Start Screen");
        startScreenCtrl.languageChoiceBox.updateLanguage();
        primaryStage.setScene(startScreen);
    }

    /**
     * updates the language of the application
     */
    public void updateLanguage(Locale locale) {
        LanguageController.getInstance().setLocale(locale);
        ConfigUtils.setPrefferedLanguage(locale.toString());
        Platform.runLater(() -> {
            settingsCtrl.updateLanguage();
            startScreenCtrl.updateLanguage();
            eventOverviewCtrl.updateLanguage();
            invitePageCtrl.updateLanguage();
            addEditParticipantsCtrl.updateLanguage();
            addEditExpenseCtrl.updateLanguage();
            loginCtrl.updateLanguage();
            adminLoginCtrl.updateLanguage();
            statsCtrl.updateLanguage();
            settleDebtsCtrl.updateLanguage();
        });
    }

    /**
     * shows the email invitation page
     */
    public void showInvitePage() {
        invitePageCtrl.init();
        primaryStage.setTitle("");
        primaryStage.setScene(invitePage);
    }


    /**
     * Show a confirmation action
     * @param message The message in the confirmation
     * @return If the user accepted or not
     */
    public boolean showConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText(message);
        alert.showAndWait();
        return alert.getResult() == ButtonType.OK;
    }

    public void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void staticShowAlert(String message, Alert.AlertType alertType) {
        instance.showAlert(message, alertType);
    }

    /**
     * Ask where to download the json file, and write it there
     * @param event
     * @param json the json string/file to download/write
     */
    public void downloadJson(Event event, String json) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JavaScript Object Notation(*.json)", "*.json"));
        fileChooser.setInitialFileName("OOPP-EventBackup-" + event.getTitle().replaceAll(" ", "_") + ".json");
        File selectedFile = fileChooser.showSaveDialog(primaryStage);

        try {
            FileWriter file = new FileWriter(selectedFile.getAbsolutePath());
            file.write(json);
            file.close();
            System.out.println("Saved the event json to:\n" + selectedFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Gets the string content of the chosen file
     * @return the json contents
     */
    public String getJsonFromFile() {
        FileChooser fileChooser = new FileChooser();

        //this is the filter for json files, if it doesn't work on linux, i don't know how to fix that
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JavaScript Object Notation(*.json)", "*.json"));

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        try {
            return Files.asCharSource(new File(selectedFile.getAbsolutePath()), Charsets.UTF_8).read();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    /**
     * Set the icon to any button
     * Implemented here so any screen can have a button with an icon
     * @param button
     * @param pngName
     */
    public void setButtonIcon(Button button, String pngName) {
        Path path = Path.of("src/main/resources/client/Icons/" + pngName + ".png");
        path = ((new File(String.valueOf(path))).exists() ? path : Path.of("client/" + path));
        ImageView imageView = new ImageView();
        if (new File(String.valueOf(path)).exists()) {
            imageView.setImage(new Image(path.toUri().toString()));
            imageView.setFitWidth(16);
            imageView.setFitHeight(16);
            button.setGraphic(imageView);
        } else {
            System.out.println("Image not found: " + path.toAbsolutePath());
        }
    }

    public void registerLanguageChoiceBox(LanguageChoiceBox languageChoiceBox) {
        languageChoiceBoxes.add(languageChoiceBox);
    }
    public void updateAllLanguageChoiceBoxes() {
        for (LanguageChoiceBox languageChoiceBox : languageChoiceBoxes) {
            languageChoiceBox.update();
        }
    }
}