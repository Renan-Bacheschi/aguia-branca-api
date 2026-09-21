package io.github.renanbacheschi.aguiabranca.error;

public class InvalidAuthenticatedUserException extends RuntimeException {

    public InvalidAuthenticatedUserException() {
        super("Token inválido ou expirado.");
    }
}
