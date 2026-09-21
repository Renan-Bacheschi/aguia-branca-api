package io.github.renanbacheschi.aguiabranca.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
