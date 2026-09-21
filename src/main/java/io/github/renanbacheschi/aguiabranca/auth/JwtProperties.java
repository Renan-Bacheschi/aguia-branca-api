package io.github.renanbacheschi.aguiabranca.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET é obrigatório.") String secret,
        @Min(value = 1, message = "JWT_EXPIRATION_MINUTES deve ser maior que zero.") long expirationMinutes) {
}
