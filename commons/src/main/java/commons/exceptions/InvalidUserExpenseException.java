package commons.exceptions;

/**
 * Thrown when the given userExpense does not correspond with an userExpense in the database
 * or the given userExpense is not a valid userExpense
 */
public class InvalidUserExpenseException extends RuntimeException {
    public InvalidUserExpenseException(String errorMessage) {
        super(errorMessage);
    }
}
