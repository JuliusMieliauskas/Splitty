package client.utils;

import client.ExpenseAction;
import client.scenes.MainCtrl;
import javafx.scene.control.Alert;

import java.util.ArrayList;

public class UndoUtils {
    private static final ArrayList<ExpenseAction> history = new ArrayList<>();
    private static int it = -1;

    public static void updateExpenseId(Long from, Long to) {
        for (ExpenseAction action : history) {
            action.updateExpenseId(from, to);
        }
    }

    /**
     * Undo an action
     */
    public static void undo() {
        System.out.println("Undoing..");
        if (it == -1) {
            MainCtrl.staticShowAlert("Cannot undo any further", Alert.AlertType.WARNING);
            return;
        }
        if (it >= history.size()) {
            it = history.size() - 1;
        }
        history.get(it--).undo();
        System.out.println("History size: " + history.size() + "; it: " + it);
    }

    /**
     * Redo an action
     */
    public static void redo() {
        System.out.println("Redoing..");
        if (it >= history.size() - 1) {
            MainCtrl.staticShowAlert("Cannot redo any further", Alert.AlertType.WARNING);
            return;
        }
        history.get(++it).redo();
        System.out.println("History size: " + history.size() + "; it: " + it);
    }

    public static void addAction(ExpenseAction action) {
        history.subList(it + 1, history.size()).clear();
        history.add(action);
        it++;
    }

    public static void resetHistory() {
        history.clear();
        it = -1;
    }


}
