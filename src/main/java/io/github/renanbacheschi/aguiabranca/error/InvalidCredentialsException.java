package io.github.renanbacheschi.aguiabranca.error;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos.");
    }
}
