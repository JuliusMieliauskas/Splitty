package client.scenes;

import client.ExpenseAction;
import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EventUtils;
import client.utils.ExpenseUtils;
import client.utils.UndoUtils;
import client.utils.UserUtils;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class AddEditExpenseCtrl implements Initializable {
    private final MainCtrl mainCtrl;

    @FXML
    private Label event;
    @FXML
    private ComboBox<String> participant;
    @FXML
    private TextField amount;
    @FXML
    private TextField title;
    @FXML
    private ScrollPane debtorsPane;
    @FXML
    private CheckBox equal;
    @FXML
    private Label whoPaidLabel;
    @FXML
    private Label whatForLabel;
    @FXML
    private Label splitBetweenWhoLabel;
    @FXML
    private Label splitEqualLabel;
    @FXML
    private Label addEditExpenseLabel;
    @FXML
    private Label needsToBeFilledInLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label currencyText;
    private Expense expense;

    private VBox debtorsBox;

    /**
     * The injected constructor
     *
     * @param mainCtrl the main controller
     */
    @Inject
    public AddEditExpenseCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainCtrl.setButtonIcon(cancelButton, "back");
    }

    /**
     * This sets the Title, and List of participants in the scene
     */
    public void init(Expense expenseGettingChanged) {
        // Set the event title
        String eventTitle = ConfigUtils.getCurrentEvent().getTitle();
        event.setText(eventTitle);
        // Set every box with the participants
        Set<User> users = null;
        users = EventUtils.getParticipants(ConfigUtils.getCurrentEvent().getId());
        List<String> usersString = users.stream().map(User::getUsername).toList();
        participant.setItems(FXCollections.observableArrayList(usersString));
        debtorsBox = new VBox();
        debtorsBox.setSpacing(4);
        for (String usr : usersString) {
            CheckBox c = new CheckBox(usr);
            Label l = new Label(" " + LanguageController.getInstance().getString("owes amount") + ": ");
            TextField t = new TextField();
            t.setPrefWidth(60);
            Label l2 = new Label(ConfigUtils.getCurrency());
            HBox hbox = new HBox(c, l, t, l2);
            debtorsBox.getChildren().add(hbox);
            if (expenseGettingChanged != null) {
                ExpenseUtils.getDebtorsOfExpense(expenseGettingChanged.getId()).stream()
                        .filter(userExpense -> userExpense.getDebtor().getUsername().equals(usr))
                        .findAny()
                        .ifPresent(userExpense -> {
                            c.setSelected(true);
                            t.setText(String.format("%.2f", userExpense.getTotalAmount() * ConfigUtils.getExchangeRate()));
                        });
            }
        }
        debtorsPane.setContent(debtorsBox);
        debtorsPane.setPannable(true);
        //set the currency text
        currencyText.setText(ConfigUtils.getCurrency());

        //clear all fields
        participant.setValue("");
        participant.getSelectionModel().clearSelection();
        amount.setText("");
        title.setText("");
        if (expenseGettingChanged != null) {
            handleEditExpense(users, expenseGettingChanged);
        }
        changeEqual();
        debtorsPane.layout();
        //equal.setSelected(false);
        addShortcuts();
    }



    private void handleEditExpense(Set<User> users, Expense expenseGettingChanged) {
        long numDistinctAmounts = 0;
        this.expense = ExpenseUtils.getExpenseById(expenseGettingChanged.getId());
        expense.getUserExpenses().stream()
                .map(UserExpense::getTotalAmount)
                .forEach(System.out::println);
        numDistinctAmounts = expense.getUserExpenses().stream()
                .map(UserExpense::getTotalAmount)
                .distinct()
                .count();
        participant.setValue(expense.getOriginalPayer().getUsername());
        participant.layout();
        title.setText(expense.getTitle());
        double owedByPayer = expense.getAmount() - expense.getUserExpenses().stream()
                .mapToDouble(UserExpense::getTotalAmount).sum();
        if (numDistinctAmounts == 1 && (owedByPayer == 0 ||
                (double) Math.round(owedByPayer * 100) / 100 ==
                        (double) Math.round(expense.getUserExpenses().stream().findAny().get().getTotalAmount() * 100) / 100)) {
            equal.setSelected(true);
            amount.setText(String.format("%.2f", expense.getAmount() * ConfigUtils.getExchangeRate()));
        }
        Set<User> usersWhoOwe = users.stream()
                .filter(user -> expense.getUserExpenses().stream()
                        .anyMatch(userExpense -> userExpense.getDebtor().getId().equals(user.getId())))
                .collect(Collectors.toSet());

        if ((equal.isSelected()) &&
                expense.getAmount() / usersWhoOwe.size() != expense.getUserExpenses().stream().findFirst().get().getTotalAmount()) {
            usersWhoOwe.add(expense.getOriginalPayer());
        }

        if (equal.isSelected()) {
            debtorsBox.getChildren().stream()
                    .filter(HBox.class::isInstance)
                    .map(HBox.class::cast)
                    .flatMap(hbox -> hbox.getChildren().stream())
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .forEach(checkBox -> {
                        String username = checkBox.getText();
                        try {
                            User user = UserUtils.getUserByName(username);
                            checkBox.setSelected(usersWhoOwe.contains(user));
                        } catch (FailedRequestException e) {
                            mainCtrl.showAlert("user doesn't exist " + username, Alert.AlertType.ERROR);
                        }
                    });
        } else {
            equal.setSelected(false);
            for (Node node : debtorsBox.getChildren()) {
                if (node instanceof HBox) {
                    setUpAmounts((HBox) node, owedByPayer);
                }
            }
        }

        participant.setAccessibleText(expense.getOriginalPayer().getUsername());
    }


    private void setUpAmounts(HBox node, Double owedByPayer) {
        HBox hbox = node;
        CheckBox c = (CheckBox) hbox.getChildren().get(0);
        TextField t = (TextField) hbox.getChildren().get(2);
        expense.getUserExpenses().forEach(userExpense -> {
            if (userExpense.getDebtor().getUsername().equals(c.getText())) {
                c.setSelected(true);
                t.setText(String.format("%.2f", userExpense.getTotalAmount() * ConfigUtils.getExchangeRate()));
            }
        });
        if (expense.getOriginalPayer().getUsername().equals(c.getText()) && owedByPayer != 0) {
            c.setSelected(true);
            t.setText(String.format("%.2f", owedByPayer * ConfigUtils.getExchangeRate()));
        }
    }

    private boolean checkFieldsFilledIn(String inputTitle) {
        if (participant.getValue() == null ||
                participant.getValue().isEmpty() ||
                (equal.isSelected() && amount.getText().isEmpty()) ||
                inputTitle.isEmpty()) {
            mainCtrl.showAlert("Fill in all the required fields. " +
                    "Without all fields filled, it is not able to add the expense.", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    /**
     * Invoked when save button is pressed in the scene
     * Save the expense to the event and the corresponding users
     */
    public void saveExpense() {
        if (expense != null) {
            updateExpense();
            return;
        }
        this.expense = null;
        String inputTitle = title.getText();

        if (!checkFieldsFilledIn(inputTitle)) {
            return;
        }

        // Getting all the participants that owe the creditor + amount
        List<Pair<User, Double>> debtorsList = calcDebtorsAndAmountsWhenSaving();
        if (debtorsList == null) {
            return;
        }
        double inputAmount = debtorsList.stream()
                .mapToDouble(Pair::getValue)
                .sum();

        // Should always work
        User ogUsr = UserUtils.getUserByName(participant.getValue());

        // create the expense
        Expense newExpense = new Expense(inputAmount, inputTitle, ogUsr, ConfigUtils.getCurrentEvent());

        // Doing the api requests to change it to the database
        Expense ex;
        try {
            ex = ExpenseUtils.createExpense(newExpense);
        } catch (FailedRequestException e) {
            mainCtrl.showAlert("Could not create new expense:\n\n" + e.getReason(), Alert.AlertType.ERROR);
            return;
        }

        List<UserExpense> userExpenseList = new ArrayList<>();
        // Add for every debtor the userExpense
        for (Pair<User, Double> usr : debtorsList) {
            if (usr.getKey().equals(ogUsr)) {
                continue;
            }
            UserExpense userExpense = new UserExpense(usr.getKey(), ex, 0, usr.getValue());

            // Doing the api requests to change it to the database
            try {
                ExpenseUtils.addUserToExpense(ex.getId(), userExpense);
            } catch (FailedRequestException e) {
                mainCtrl.showAlert("Could not add " + usr.getKey().getUsername() + "expense:\n\n" + e.getReason(), Alert.AlertType.ERROR);
                continue;
            }
            userExpenseList.add(userExpense);
        }

        UndoUtils.addAction(new ExpenseAction(ExpenseAction.Type.CREATION, ex, userExpenseList));


        System.out.println("Saved expense with this info:\n" +
                "Payer: " + ogUsr.getUsername() + "\n" +
                "Amount: " + inputAmount + "\n" +
                "Title: " + inputTitle + "\n" +
                "Debtors: " + debtorsList.stream().map(o -> o.getKey().getUsername()).toList() + "\n" +
                "\nTo event with id " + ConfigUtils.getCurrentEvent().getId());
        equal.setSelected(false);
        mainCtrl.showEventOverview();


    }

    private void updateExpense() {
        debtorsBox.getChildren().removeAll();
        String inputTitle = title.getText();

        if (!checkFieldsFilledIn(inputTitle)) {
            return;
        }

        // Getting all the participants that owe the creditor + amount
        List<Pair<User, Double>> debtorsList = calcDebtorsAndAmountsWhenSaving();
        if (debtorsList == null) {
            return;
        }
        double inputAmount = debtorsList.stream()
                .mapToDouble(Pair::getValue)
                .sum();

        // Should always work
        User ogUsr = UserUtils.getUserByName(participant.getValue());
        debtorsList = debtorsList.stream()
                .filter(pair -> !pair.getKey().equals(ogUsr))
                .toList();
        expense.setAmount(inputAmount);
        expense.setTitle(inputTitle);
        expense.setOriginalPayer(ogUsr);
        Expense old = ExpenseUtils.getExpenseById(expense.getId());
        ExpenseUtils.updateExpense(expense);
        List<UserExpense> oldUserExpenses = ExpenseUtils.getDebtorsOfExpense(old.getId());
        removeRemovedDebtors(expense, debtorsList);
        addUpdateDebtors(debtorsList, ogUsr);


        UndoUtils.addAction(
                new ExpenseAction(ExpenseAction.Type.UPDATEBOTH,
                        old, ExpenseUtils.getExpenseById(old.getId()),
                        oldUserExpenses, ExpenseUtils.getDebtorsOfExpense(old.getId())));
        mainCtrl.showExpenseView(expense);
        this.expense = null;
        equal.setSelected(false);
    }

    private void addUpdateDebtors(List<Pair<User, Double>> debtorsList, User ogUsr) {
        for (Pair<User, Double> usr : debtorsList) {
            if (usr.getKey().equals(ogUsr)) {
                continue;
            }
            UserExpense userExpense = new UserExpense(usr.getKey(), expense, 0, usr.getValue());

            try {
                ExpenseUtils.addUserToExpense(expense.getId(), userExpense);
            } catch (FailedRequestException e) {
                double paidAmount = 0;
                Optional<UserExpense> optionalUserExpense = UserUtils.getUserExpenses(usr.getKey()).stream()
                        .filter(ue -> ue.getExpense().getId().equals(expense.getId())).findFirst();
                if (optionalUserExpense.isPresent()) {
                    paidAmount = optionalUserExpense.get().getPaidAmount();
                } else {
                    paidAmount = 0;
                }
                userExpense.setPaidAmount(paidAmount);
                userExpense.setTotalAmount(usr.getValue());
                //ExpenseUtils.deleteUserFromExpense(expense.getId(), usr.getKey());
                //ExpenseUtils.addUserToExpense(expense.getId(), userExpense);
                UserUtils.updateUserExpense(userExpense);
            }
        }
    }

    private void removeRemovedDebtors(Expense expense, List<Pair<User, Double>> debtorsList) {
        Set<User> debtorUsers = debtorsList.stream()
                .map(Pair::getKey)
                .collect(Collectors.toSet());
        expense.getUserExpenses().forEach(userExpense -> {
            User debtor = userExpense.getDebtor();
            if (!debtorUsers.contains(debtor)) {
                ExpenseUtils.deleteUserFromExpense(expense.getId(), debtor);
            }
        });
    }

    /**
     * Reads from the input fields, returns null if there was some wrong input
     *
     * @return a list with a participants involved and their debt amount
     */
    public List<Pair<User, Double>> calcDebtorsAndAmountsWhenSaving() {
        List<Pair<User, Double>> debtorsList;
        double inputAmount;
        if (equal.isSelected()) {
            try {
                inputAmount = Double.parseDouble(amount.getText()) / ConfigUtils.getExchangeRate();
            } catch (Exception e) {
                mainCtrl.showAlert("Amount must be a number, for decimals use a '.'", Alert.AlertType.ERROR);
                return null;
            }
            debtorsList = debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .map(o -> new Pair<>((CheckBox) o.getChildren().get(0), (TextField) o.getChildren().get(2)))
                    .filter(o -> o.getKey().isSelected())
                    .map(o -> {
                        try {
                            return new Pair<>(UserUtils.getUserByName(o.getKey().getText()), Double.parseDouble("0"));
                        } catch (FailedRequestException e) {
                            mainCtrl.showAlert("Could not find user with username " + o.getKey().getText(), Alert.AlertType.ERROR);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            if (debtorsList.isEmpty()) {
                System.out.println("Fill in all the required fields. Without all fields filled, it is not able to add the expense");
                mainCtrl.showAlert("Choose some debtors", Alert.AlertType.WARNING);
                return null;
            }
            double splitAmount = inputAmount / debtorsList.size();
            debtorsList = debtorsList.stream()
                    .map(o -> new Pair<>(o.getKey(), splitAmount))
                    .toList();
        } else {
            debtorsList = debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .map(o -> new Pair<>((CheckBox) o.getChildren().get(0), (TextField) o.getChildren().get(2)))
                    .filter(o -> o.getKey().isSelected())
                    .map(o -> {
                        try {
                            if (o.getValue().getText() != null) {
                                try {
                                    Double.parseDouble(o.getValue().getText());
                                } catch (NumberFormatException e) {
                                    mainCtrl.showAlert(LanguageController.getInstance().
                                            getString("amount cannot be empty"), Alert.AlertType.ERROR);
                                    return null;
                                }

                                return new Pair<>(UserUtils.getUserByName(o.getKey().getText()),
                                        Double.parseDouble(o.getValue().getText()) / ConfigUtils.getExchangeRate());
                            }

                        } catch (FailedRequestException e) {
                            mainCtrl.showAlert(LanguageController.getInstance().
                                    getString("could not find user with username") + o.getKey().getText(), Alert.AlertType.ERROR);
                            return null;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            if (debtorsList.isEmpty()) {
                mainCtrl.showAlert(LanguageController.getInstance().
                        getString("you have to add at least 1 user as debtor"), Alert.AlertType.WARNING);
                return null;
            }
        }
        return debtorsList;
    }

    /**
     * Runs if the checkbox for equal split is pressed/unpressed
     * Changed visibility and usability of certain fields and buttons
     */
    public void changeEqual() {
        if (equal.isSelected()) {
            amount.setEditable(true);
            debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .forEach(o -> o.getChildren().get(1).setVisible(false));
            debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .forEach(o -> o.getChildren().get(2).setVisible(false));
            debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .forEach(o -> o.getChildren().get(3).setVisible(false));
        } else {
            amount.setText("");
            amount.setEditable(false);
            debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .forEach(o -> o.getChildren().get(1).setVisible(true));
            debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .forEach(o -> o.getChildren().get(2).setVisible(true));
            debtorsBox.getChildren()
                    .stream()
                    .map(o -> (HBox) o)
                    .forEach(o -> o.getChildren().get(3).setVisible(true));
        }
    }

    /**
     * cancels adding or editing expense and returns to previous page.
     */
    public void cancel() {
        System.out.println("Cancelled the creation of an expense, now redirect to the event overview page");
        equal.setSelected(false);
        if (expense != null) {
            mainCtrl.showExpenseView(expense);
            this.expense = null;
        } else {
            mainCtrl.showEventOverview();
        }
    }

    /**
     * Updates the language of the scene
     */
    public void updateLanguage() {
        System.out.println("Updating language in AddEditExpenseCtrl");
        whoPaidLabel.setText(LanguageController.getInstance().getString("Who Paid") + "?*");
        whatForLabel.setText(LanguageController.getInstance().getString("What For") + "?*");
        splitBetweenWhoLabel.setText(LanguageController.getInstance().getString("Split between who") + "?*");
        splitEqualLabel.setText(LanguageController.getInstance().getString("Split equal") + "?");
        addEditExpenseLabel.setText(LanguageController.getInstance().getString("Add/edit expense"));
        needsToBeFilledInLabel.setText("*" + LanguageController.getInstance().getString("Needs to be filled in"));
        saveButton.setText(LanguageController.getInstance().getString("save"));
        cancelButton.setText(LanguageController.getInstance().getString("return"));
        equal.setText(LanguageController.getInstance().getString("Yes, with total amount"));
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = event.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancel();
                        event.consume();
                    }
                });
            }
        });
        participant.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                participant.show();
            }
        });
    }
}
