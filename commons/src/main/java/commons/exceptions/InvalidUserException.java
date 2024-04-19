package commons.exceptions;

/**
 * Thrown when the given user does not correspond with a user in the database
 * or the given user is not a valid user
 */
public class InvalidUserException extends RuntimeException {
    public InvalidUserException(String errorMessage) {
        super(errorMessage);
    }
}
