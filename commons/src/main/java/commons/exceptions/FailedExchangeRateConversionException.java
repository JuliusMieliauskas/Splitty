package commons.exceptions;

/**
 * Thrown when an API request is made and it fails
 */
public class FailedExchangeRateConversionException extends Exception {
    public FailedExchangeRateConversionException(String errorMessage) {
        super(errorMessage);
    }
}
