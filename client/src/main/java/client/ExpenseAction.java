package client;

import client.utils.ExpenseUtils;
import client.utils.UndoUtils;
import commons.Expense;
import commons.UserExpense;

import java.util.List;
import java.util.Objects;

public class ExpenseAction {
    public enum Type {
      UPDATE, UPDATEUSEREXPENSES, DELETED, CREATION, UPDATEBOTH, ACTIONGROUP
    };
    private Type type;
    private Expense e1;
    private Expense e2;
    private List<UserExpense> uel1;
    private List<UserExpense> uel2;
    private List<ExpenseAction> multipleActions;

    /**
     * Constructor for type == actiongroup
     * @param type type == actiongroup
     * @param actions the actions combined in 1 group
     */
    public ExpenseAction(Type type, List<ExpenseAction> actions) {
        if (type != Type.ACTIONGROUP) {
            throw new RuntimeException("Incorrect construction of expense action");
        }
        this.type = type;
        multipleActions = actions;
    }

    /**
     * Constructor for type == update
     * @param type type == update
     * @param from the old state of the expense
     * @param to the resulting state of the expense
     */
    public ExpenseAction(Type type, Expense from, Expense to) {
        if (type != Type.UPDATE) {
            throw new RuntimeException("Incorrect construction of expense action");
        }
        this.type = type;
        e1 = from;
        e2 = to;
    }

    /**
     * Constructor for type == update
     * @param type type == update
     * @param from the old state of the expense
     * @param to the resulting state of the expense
     */
    public ExpenseAction(Type type, Expense from, Expense to, List<UserExpense> from2, List<UserExpense> to2) {
        if (type != Type.UPDATEBOTH) {
            throw new RuntimeException("Incorrect construction of expense action");
        }
        this.type = type;
        e1 = from;
        e2 = to;
        uel1 = from2;
        uel2 = to2;
    }

    /**
     * Constructor for type == deleted/creation
     * @param type type == deleted/creation
     * @param expense the deleted/created expense
     */
    public ExpenseAction(Type type, Expense expense, List<UserExpense> userExpenses) {
        if (type != Type.CREATION && type != Type.DELETED) {
            throw new RuntimeException("Incorrect construction of expense action");
        }
        this.type = type;
        e1 = expense;
        uel1 = userExpenses;
    }

    /**
     * Constructor for type == updateuserexpenses
     * @param type type == updateuserexpenses
     * @param expense The expense the userexpenses are a part of
     * @param from the old state of the userexpenses
     * @param to the resulting state of the userexpenses
     */
    public ExpenseAction(Type type, Expense expense, List<UserExpense> from, List<UserExpense> to) {
        if (type != Type.UPDATEUSEREXPENSES) {
            throw new RuntimeException("Incorrect construction of expense action");
        }
        this.type = type;
        this.e1 = expense;
        uel1 = from;
        uel2 = to;
    }

    public Type getType() {
        return type;
    }

    /**
     * If one of the other undo action changes the id of a certain expense, it should be mapped here
     * @param from old id
     * @param to new id
     */
    public void updateExpenseId(Long from, Long to) {
        if (type == Type.ACTIONGROUP) {
            for (ExpenseAction ea : multipleActions) {
                ea.updateExpenseId(from, to);
            }
        }
        if (e1 != null && Objects.equals(e1.getId(), from)) {
            e1.setId(to);
        }
        if (e2 != null && Objects.equals(e2.getId(), from)) {
            e2.setId(to);
        }
    }

    /**
     * Undo this action
     */
    public void undo() {
        if (type == Type.UPDATE) {
            ExpenseUtils.updateExpense(e1);
        } else if (type == Type.CREATION) {
            ExpenseUtils.removeExpense(e1.getId());
        } else if (type == Type.DELETED) {
            createExpense();
        } else if (type == Type.UPDATEUSEREXPENSES) {
            uel1.forEach(x -> x.setExpense(e1));
            ExpenseUtils.changeUserExpense(e1.getId(), uel1);
        } else if (type == Type.UPDATEBOTH) {
            ExpenseUtils.updateExpense(e1);
            uel1.forEach(x -> x.setExpense(e1));
            ExpenseUtils.changeUserExpense(e1.getId(), uel1);
        } else if (type == Type.ACTIONGROUP) {
            for (int i = multipleActions.size() - 1; i >= 0; i--) {
                multipleActions.get(i).undo();
            }
        }
    }

    /**
     * redo this action
     */
    public void redo() {
        if (type == Type.UPDATE) {
            ExpenseUtils.updateExpense(e2);
        } else if (type == Type.CREATION) {
            createExpense();
        } else if (type == Type.DELETED) {
            ExpenseUtils.removeExpense(e1.getId());
        } else if (type == Type.UPDATEUSEREXPENSES) {
            uel2.forEach(x -> x.setExpense(e1));
            ExpenseUtils.changeUserExpense(e1.getId(), uel2);
        } else if (type == Type.UPDATEBOTH) {
            ExpenseUtils.updateExpense(e2);
            uel2.forEach(x -> x.setExpense(e2));
            ExpenseUtils.changeUserExpense(e2.getId(), uel2);
        } else if (type == Type.ACTIONGROUP) {
            for (ExpenseAction action : multipleActions) {
                action.redo();
            }
        }
    }

    private void createExpense() {
        Long newExId = ExpenseUtils.createExpense(e1).getId();
        UndoUtils.updateExpenseId(e1.getId(), newExId);
        for (UserExpense ue : uel1) {
            ue.setExpense(e1);
            ExpenseUtils.addUserToExpense(e1.getId(), ue);
        }
    }
}
