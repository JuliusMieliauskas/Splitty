package commons.exceptions;

/**
 * Thrown when the given event does not correspond with an event in the database
 * or the given event is not a valid event
 */
public class InvalidEventException extends RuntimeException {
    /**
     * Basic constructor
     */
    public InvalidEventException(String errorMessage) {
        super(errorMessage);
    }
}
