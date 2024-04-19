package commons.exceptions;

/**
 * Thrown if a file is read, but the information in the file is in the incorrect format
 */
public class InvalidFileFormatException extends Exception {
    public InvalidFileFormatException(String errorMessage) {
        super(errorMessage);
    }
}
