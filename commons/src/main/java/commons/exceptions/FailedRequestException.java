package commons.exceptions;

public class FailedRequestException extends RuntimeException {
    private final int status;
    private final String exceptionType;
    private final String reason;

    /**
     * Creates a new exception based on a failed api request
     */
    public FailedRequestException(int status, String reason) {
        super("Request failed with status " + status + ":\n" + reason);
        this.status = status;
        int i = reason.indexOf("Exception:");
        if (i != -1) {
            this.exceptionType = reason.substring(0, i);
            reason = reason.substring(i + 10);
            this.reason = (reason.startsWith("\n") ? reason.substring(1) : reason);
        } else {
            this.reason = reason;
            this.exceptionType = null;
        }
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
