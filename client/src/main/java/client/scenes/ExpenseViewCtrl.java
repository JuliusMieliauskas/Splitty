package client.scenes;

import client.ExpenseAction;
import client.LongPollingService;
import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EmailUtils;
import client.utils.ExpenseUtils;
import client.utils.UndoUtils;
import com.google.inject.Inject;
import commons.Expense;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class ExpenseViewCtrl implements Initializable {
    private final MainCtrl mainCtrl;
    private LongPollingService longPollingService;

    @FXML
    private Label ibanlbl;
    @FXML
    private Label iban;
    @FXML
    private Label titleLabel;
    @FXML
    private Label tagLabel;
    @FXML
    private Label originalPayerLabel;
    @FXML
    private TableView<UserExpense> userExpenseTable;
    @FXML
    private TableColumn<UserExpense, String> userNameColumn;
    @FXML
    private TableColumn<UserExpense, Double> paidColumn;
    @FXML
    private TableColumn<UserExpense, Double> totalColumn;
    @FXML
    private Button reminderBtn;
    @FXML
    public TextField paidAmount;
    @FXML
    public Label paidAmountMsg;
    @FXML
    private Button singleRemindButton;

    @FXML
    private Label categoryLabel;
    @FXML
    private Button returnButton;
    @FXML
    private Label originalPayer;
    @FXML
    private Button editExpenseButton;
    private Expense expense;

    @Inject
    public ExpenseViewCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainCtrl.setButtonIcon(returnButton, "back");

        try {
            userNameColumn.setCellValueFactory(
                    cellData -> new SimpleStringProperty(
                            cellData.getValue().getDebtor().getUsername()
                    ));
            paidColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPaidAmount()));

            paidColumn.setCellValueFactory(cellData -> {
                double convertedPaidAmount = cellData.getValue().getPaidAmount() * ConfigUtils.getExchangeRate();
                double roundedPaidAmount = Math.round(convertedPaidAmount * 100) / 100.0;
                return new SimpleObjectProperty<>(roundedPaidAmount);
            });
            totalColumn.setCellValueFactory(cellData -> {
                double convertedTotalAmount = cellData.getValue().getTotalAmount() * ConfigUtils.getExchangeRate();
                double roundedTotalAmount = Math.round(convertedTotalAmount * 100) / 100.0;
                return new SimpleObjectProperty<>(roundedTotalAmount);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        addShortcuts();

    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = titleLabel.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        returnToEvent();
                        event.consume();
                    }
                    if (event.getCode() == KeyCode.E) {
                        mainCtrl.showExpenseAdd(expense);
                    }
                    if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                        UndoUtils.undo();
                        updateStageIfExpenseExists();
                    }
                    if (event.isControlDown() && event.getCode() == KeyCode.Y) {
                        UndoUtils.redo();
                        updateStageIfExpenseExists();
                    }
                });
            }
        });

        btnVisibility();

    }

    private void btnVisibility() {
        //greys out the send reminders buttons as required
        reminderBtn.setDisable(ConfigUtils.getUserEmail() == null ||
                ConfigUtils.getEmailPassword() == null);

        singleRemindButton.setDisable(true);

        userExpenseTable.getSelectionModel().selectedItemProperty().addListener((observableValue, event, t1) -> {
            reminderBtn.setDisable(ConfigUtils.getUserEmail() == null ||
                    ConfigUtils.getEmailPassword() == null);
            singleRemindButton.setDisable(ConfigUtils.getUserEmail() == null ||
                    ConfigUtils.getEmailPassword() == null ||
                    t1 == null ||
                    ConfigUtils.getEmail(t1.getDebtor().getId()) == null);

        });
    }

    private void updateStageIfExpenseExists() {
        Platform.runLater(() -> {
            longPollingService.unlink();
            longPollingService.cancel();
            longPollingService.reset();
            try {
                init(
                        ExpenseUtils.getExpenseById( // will throw error if does not exist anymore
                                getExpense().getId()));
            } catch (FailedRequestException e) {
                mainCtrl.showEventOverview();
            }
        });
    }

    /**
     * Sets this.expense to the chosen expense and initializes long-polling
     *
     * @param selectedExpense
     */
    public void setExpense(Expense selectedExpense) {
        longPollingService.link(selectedExpense.getId());
        this.expense = selectedExpense;
        updateUI();
        reminderBtn.setDisable(ConfigUtils.getUserEmail() == null);
        paidAmount.setVisible(false);
        paidAmountMsg.setVisible(false);

        // make send reminder option for when an event is selected
        userExpenseTable.getSelectionModel().selectedItemProperty().addListener((observableValue, event, t1) -> {
            if (t1 != null) {
                reminderBtn.setVisible(true);
                paidAmountMsg.setVisible(true);
                paidAmount.setVisible(true);
                double convertedPaidAmount = t1.getPaidAmount() * ConfigUtils.getExchangeRate();
                double roundedPaidAmount = Math.round(convertedPaidAmount * 100) / 100.0;
                paidAmount.setText(String.valueOf(roundedPaidAmount));
                singleRemindButton.setVisible(true);
                singleRemindButton.setDisable(ConfigUtils.getUserEmail() == null ||
                        ConfigUtils.getEmail(t1.getDebtor().getId()) == null);
            } else {
                reminderBtn.setVisible(false);
                paidAmountMsg.setVisible(false);
                paidAmount.setVisible(false);
                singleRemindButton.setVisible(false);
            }
        });

        paidAmount.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    double amount = -1;
                    UserExpense ue = userExpenseTable.getSelectionModel().getSelectedItem();
                    try {
                        if ("-".equals(paidAmount.getText().strip())) {
                            amount = ue.getTotalAmount();
                        } else {
                            amount = Double.parseDouble(paidAmount.getText()) / ConfigUtils.getExchangeRate();
                        }
                        ue.setPaidAmount(amount);
                        List<UserExpense> old = ExpenseUtils.getDebtorsOfExpense(expense.getId());
                        ExpenseUtils.editDebtorOfExpense(ue);
                        userExpenseTable.refresh();
                        UndoUtils.addAction(
                                new ExpenseAction(ExpenseAction.Type.UPDATEUSEREXPENSES, expense, old,
                                    ExpenseUtils.getDebtorsOfExpense(expense.getId())));
                    } catch (FailedRequestException e) {
                        mainCtrl.showAlert("Invalid paid amount\n\nValue " + amount + " not in range [0, " + ue.getTotalAmount() + "]",
                                Alert.AlertType.WARNING);
                    } catch (Exception e) {
                        mainCtrl.showAlert("Invalid paid amount\n\n" + e.getMessage(), Alert.AlertType.WARNING);
                    }
                }
            }
        });
        Platform.runLater(this::updateLanguage);
    }

    public Expense getExpense() {
        return this.expense;
    }
    /**
     * Init the page for a certain expense
     */
    public void init(Expense selectedExpense) {
        if (longPollingService == null) {
            longPollingService = new LongPollingService(ConfigUtils.getServerUrl(), this);
        }
        userExpenseTable.getSelectionModel().clearSelection();
        this.expense = selectedExpense;
        setExpense(expense);
        updateUI();
        paidAmount.setVisible(false);
        paidAmountMsg.setVisible(false);
        singleRemindButton.setVisible(false);

        String ibanTxt = ConfigUtils.getIban(selectedExpense.getOriginalPayer().getId());
        ibanlbl.setVisible(ibanTxt != null);
        iban.setVisible(ibanTxt != null);
        if (ibanTxt != null) {
            iban.setText(ibanTxt);
        }
    }

    private void updateUI() {
        if (expense != null) { // Ensure the selectedExpense is not null
            titleLabel.setText(expense.getTitle());
            tagLabel.setText(translateToSelectedLanguage(expense.getTag()));
            double ogPayerAmount = expense.getAmount();
            for (UserExpense ue : ExpenseUtils.getDebtorsOfExpense(expense.getId())) {
                ogPayerAmount -= ue.getTotalAmount();
            }
            originalPayer.setText(expense.getOriginalPayer().getUsername() + " (" +
                    String.format("%.2f", ogPayerAmount * ConfigUtils.getExchangeRate()) + " " + ConfigUtils.getCurrency() + ")");

            ObservableList<UserExpense> userExpenses = FXCollections.observableArrayList(ExpenseUtils.getDebtorsOfExpense(expense.getId()));
            userExpenseTable.setItems(FXCollections.observableArrayList());
            userExpenseTable.setItems(userExpenses);
            userExpenseTable.refresh();
            singleRemindButton.setDisable(true);
        }
    }

    /**
     * Updates the shown list of userExpenses, also called by long polling service
     *
     * @param userExpenseSet The new user Expense set
     */
    public void updateUserExpenses(Set<UserExpense> userExpenseSet) {
        this.expense = ExpenseUtils.getExpenseById(expense.getId());

        Platform.runLater(this::updateUI);
    }

    /**
     * Sets primary stage to mainctrl
     */
    public void returnToEvent() {
        longPollingService.unlink();
        longPollingService.cancel();
        longPollingService.reset();
        paidAmount.setVisible(false);
        paidAmountMsg.setVisible(false);
        singleRemindButton.setVisible(false);
        mainCtrl.showEventOverview();
    }

    /**
     * sends a reminder to a selected user
     */
    public void sendReminderOne() {
        try {
            EmailUtils.sendReminderOne(
                    ConfigUtils.getUserEmail(),
                    ConfigUtils.getEmailPassword(),
                    ConfigUtils.getEmail(userExpenseTable.getSelectionModel().getSelectedItem().getDebtor().getId()),
                    expense.getEvent(),
                    userExpenseTable.getSelectionModel().getSelectedItem());
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Successfully sent email"),
                    Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Email did not send"),
                    Alert.AlertType.INFORMATION);
        }
    }

    /**
     * sends a reminder to all users
     */
    public void sendReminderMany() {

        try {
            EmailUtils.sendReminderMany(
                    ConfigUtils.getUserEmail(),
                    ConfigUtils.getEmailPassword(),
                    userExpenseTable.getItems(),
                    expense.getEvent(),
                    expense);
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Successfully sent email"),
                    Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            mainCtrl.showAlert(
                    LanguageController.getInstance().getString("Email did not send"),
                    Alert.AlertType.INFORMATION);
        }

    }

    /**
     * Go to edit expense view
     */
    public void editExpense() {
        longPollingService.unlink();
        longPollingService.cancel();
        longPollingService.reset();
        mainCtrl.showExpenseAdd(expense);
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

    /**
     * updates the language of the scene
     */
    public void updateLanguage() {
        originalPayerLabel.setText(LanguageController.getInstance().getString("Original Payer"));
        categoryLabel.setText(LanguageController.getInstance().getString("Category"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
        userNameColumn.setText(LanguageController.getInstance().getString("Username"));
        paidColumn.setText(LanguageController.getInstance().getString("Paid") + " (" + ConfigUtils.getCurrency() + ")");
        totalColumn.setText(LanguageController.getInstance().getString("Amount") + " (" + ConfigUtils.getCurrency() + ")");
        reminderBtn.setText(LanguageController.getInstance().getString("Remind everyone"));
        singleRemindButton.setText(LanguageController.getInstance().getString("Send reminder"));
        paidAmountMsg.setText(LanguageController.getInstance().getString("Paid amount"));
        editExpenseButton.setText(LanguageController.getInstance().getString("edit"));
        ibanlbl.setText(LanguageController.getInstance().getString("iban"));
    }
}
