package commons.exceptions;

/**
 * Thrown when the given expense does not correspond with an expense in the database
 * or the given expense is not a valid expense
 */
public class InvalidExpenseException extends RuntimeException {
    public InvalidExpenseException(String errorMessage) {
        super(errorMessage);
    }
}
