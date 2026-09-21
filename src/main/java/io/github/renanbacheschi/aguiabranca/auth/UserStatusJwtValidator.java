package io.github.renanbacheschi.aguiabranca.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import io.github.renanbacheschi.aguiabranca.user.UserRepository;

@Component
public class UserStatusJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_USER = new OAuth2Error(
            "invalid_token", "The token user is no longer valid.", null);

    private final UserRepository userRepository;

    public UserStatusJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String userId = token.getClaimAsString("userId");
        String role = token.getClaimAsString("role");

        if (userId == null || role == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_USER);
        }

        boolean validUser = userRepository.findById(userId)
                .filter(user -> user.isActive() && user.getRole().name().equals(role))
                .isPresent();

        return validUser
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_USER);
    }
}
