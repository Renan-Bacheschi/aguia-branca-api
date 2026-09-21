package io.github.renanbacheschi.aguiabranca.error;

public class EmailConflictException extends RuntimeException {

    public EmailConflictException(String email) {
        super("E-mail já cadastrado: " + email);
    }

    public EmailConflictException(String email, Throwable cause) {
        super("E-mail já cadastrado: " + email, cause);
    }
}
