package io.github.renanbacheschi.aguiabranca.error;

public class AnalysisServiceUnavailableException extends RuntimeException {

    public AnalysisServiceUnavailableException(String message) {
        super(message);
    }

    public AnalysisServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
