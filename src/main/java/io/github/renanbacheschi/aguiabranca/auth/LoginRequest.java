package io.github.renanbacheschi.aguiabranca.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.github.renanbacheschi.aguiabranca.user.UserDocument;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail deve ser válido.")
        String email,
        @NotBlank(message = "A senha é obrigatória.")
        String password) {

    public LoginRequest {
        email = UserDocument.normalizeEmail(email);
    }
}
