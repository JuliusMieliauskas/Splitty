package client.scenes;

import client.ExpenseAction;
import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EventUtils;
import client.utils.ExpenseUtils;
import client.utils.UndoUtils;
import client.utils.UserUtils;
import com.google.inject.Inject;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SettleDebtsCtrl implements Initializable {

    private final MainCtrl mainCtrl;
    @FXML
    public Button settlebtn;

    private Map<User, Double> net;
    private List<Pair<Expense, UserExpense>> newDebts;

    @FXML
    private TreeView<String> tree;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Button returnButton;

    @Inject
    public SettleDebtsCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    /**
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     *                  the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainCtrl.setButtonIcon(returnButton, "back");

        Platform.runLater(this::updateLanguage);
        addShortcuts();
    }

    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = descriptionLabel.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    System.out.println("Key pressed: " + event.getCode());
                    if (event.getCode() == KeyCode.ESCAPE) {
                        back();
                        event.consume();
                    }
                });
            }
        });
    }

    /**
     * Calculates all the needed transactions again
     */
    public void refreshTransactions() {
        newDebts = new ArrayList<>();
        List<List<User>> usersNetAmount = calcNetPerUser();

        TreeItem<String> root = new TreeItem<String>("this is not shown");
        root.setExpanded(true);
        while (!usersNetAmount.get(0).isEmpty()) {
            User debtor = usersNetAmount.get(0).getFirst();
            User creditor = usersNetAmount.get(1).getFirst();
            double amount = Math.min(Math.abs(net.get(debtor)), Math.abs(net.get(creditor)));
            double amountRounded = Math.round(amount * 100.) / 100.; // round to 2 decimals

            Expense newEx = new Expense(amountRounded,
                    "Settle " + creditor.getUsername(),
                    creditor,
                    ConfigUtils.getCurrentEvent()
            );
            newEx.setTag("Settle");
            newDebts.add(new Pair<>(newEx,
                    new UserExpense(debtor, newEx, 0., amountRounded)));
            TreeItem<String> branch = new TreeItem<String>(debtor.getUsername() + " ---" +
                    String.format("%.2f", amountRounded * ConfigUtils.getExchangeRate()) + " " +
                    ConfigUtils.getCurrency() + "---> " + creditor.getUsername());
            String iban = ConfigUtils.getIban(creditor.getId());
            TreeItem<String> item = new TreeItem<String>(creditor.getUsername() + "'s iban: " +
                    (iban == null ? "UNKNOWN (add one in the edit participants menu)" : iban));
            branch.getChildren().add(item);
            root.getChildren().add(branch);

            net.put(debtor, net.get(debtor) + amount);
            net.put(creditor, net.get(creditor) - amount);
            if (Math.abs(net.get(debtor)) < 1e-3) {
                usersNetAmount.get(0).removeFirst();
            }
            if (Math.abs(net.get(creditor)) < 1e-3) {
                usersNetAmount.get(1).removeFirst();
            }
        }
        tree.setRoot(root);
        tree.setShowRoot(false);
    }

    /**
     * Calculates the net debt for every user in the event
     * @return a list of 2 List<User>
     * where the first List<User> are all users who are in debt
     * and the second List<User> are all users who need to get money
     */
    public List<List<User>> calcNetPerUser() {
        net = new HashMap<>();
        List<User> userList = EventUtils.getUsersOfEvent(ConfigUtils.getCurrentEvent().getId());
        List<Expense> expenseList = EventUtils.getExpensesOfEvent(ConfigUtils.getCurrentEvent().getId());

        for (User user : userList) {
            net.put((User) user, (double) 0);
            System.out.println(user);
        }

        // Calculating the net amount that each user needs to pay / needs to get, is fully based on the UserExpenses,
        // because then it always sum up to 0 even if the originalPayer is also a debtor
        for (User user : userList) {
            List<UserExpense> userExpenseList = UserUtils.getUserExpenses(user)
                    .stream()
                    .filter(o -> expenseList.contains(o.getExpense()))
                    .toList();
            for (UserExpense userExpense : userExpenseList) {
                net.put(user,
                        net.get(user) -
                                userExpense.getTotalAmount() +
                                userExpense.getPaidAmount());
                net.put(userExpense.getExpense().getOriginalPayer(),
                        net.get(userExpense.getExpense().getOriginalPayer()) +
                                userExpense.getTotalAmount() -
                                userExpense.getPaidAmount());
            }
        }

        List<List<User>> output = new ArrayList<>();
        output.add(new ArrayList<>()); // first is for everyone in debt (-net)
        output.add(new ArrayList<>()); // second is for everyone who should get money (+net)
        for (User user : userList) {
            if (net.get(user) < 0) {
                output.get(0).add(user);
            } else if (net.get(user) > 0) {
                output.get(1).add(user);
            }
        }

        return output;
    }

    public void back() {
        System.out.println("going back to event page");
        mainCtrl.showEventOverview();
    }

    /**
     * simplify all the current debts to n-1 transactions
     */
    public void settle() {
        if (!mainCtrl.showConfirmation("Are you sure you want to simplify expenses?")) {
            return;
        }
        List<ExpenseAction> actions = new ArrayList<>();
        for (Expense expense : EventUtils.getExpensesOfEvent(ConfigUtils.getCurrentEvent().getId())) {
            actions.add(new ExpenseAction(ExpenseAction.Type.DELETED,
                    expense,
                    ExpenseUtils.getDebtorsOfExpense(expense.getId())
                    ));
            ExpenseUtils.removeExpense(expense.getId());
        }
        for (Pair<Expense, UserExpense> debt : newDebts) {
            UserExpense userExpense = debt.getValue();
            Expense expense = ExpenseUtils.createExpense(debt.getKey());
            userExpense.setExpense(expense);
            ExpenseUtils.addUserToExpense(userExpense.getExpense().getId(), userExpense);
            actions.add(new ExpenseAction(ExpenseAction.Type.CREATION,
                    expense,
                    List.of(userExpense)
            ));
        }
        UndoUtils.addAction(new ExpenseAction(ExpenseAction.Type.ACTIONGROUP, actions));
        back();
    }

    /**
     * updates the language of the scene
     */
    public void updateLanguage() {
        descriptionLabel.setText(LanguageController.getInstance().getString("Transactions needed to settle debts"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
        settlebtn.setText(LanguageController.getInstance().getString("Settle"));
    }
}
