package io.github.renanbacheschi.aguiabranca.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import io.github.renanbacheschi.aguiabranca.error.InvalidAuthenticatedUserException;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

public record AuthenticatedUserContext(String userId, UserRole role) {

    public static AuthenticatedUserContext from(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");
        if (userId == null || role == null) {
            throw new InvalidAuthenticatedUserException();
        }
        try {
            return new AuthenticatedUserContext(userId, UserRole.valueOf(role));
        } catch (IllegalArgumentException exception) {
            throw new InvalidAuthenticatedUserException();
        }
    }
}
