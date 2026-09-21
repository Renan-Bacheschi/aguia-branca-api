package io.github.renanbacheschi.aguiabranca.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRepository;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTests {

    private static final Instant ISSUED_AT = Instant.parse("2026-09-20T12:00:00Z");
    private static final SecretKey SECRET_KEY = new SecretKeySpec(new byte[32], "HmacSHA256");

    private UserRepository userRepository;
    private AuthenticatedUser authenticatedUser;
    private UserDocument userDocument;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDocument = new UserDocument(
                "user-1",
                "Demo Operator",
                "operator@demo.com",
                "hash",
                UserRole.OPERATOR,
                true,
                ISSUED_AT,
                ISSUED_AT);
        authenticatedUser = AuthenticatedUser.from(userDocument);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userDocument));
    }

    @Test
    void generateTokenCreatesExpectedClaimsAndExpiration() {
        JwtTokenService tokenService = tokenService(ISSUED_AT, ISSUED_AT);

        Jwt jwt = tokenService.validateToken(tokenService.generateToken(authenticatedUser));

        assertThat(jwt.getSubject()).isEqualTo("operator@demo.com");
        assertThat(jwt.getClaimAsString("userId")).isEqualTo("user-1");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("OPERATOR");
        assertThat(jwt.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED_AT.plus(Duration.ofMinutes(120)));
        assertThat(tokenService.getExpirationSeconds()).isEqualTo(7200);
    }

    @Test
    void validateTokenRejectsExpiredToken() {
        JwtTokenService issuer = tokenService(ISSUED_AT, ISSUED_AT);
        String token = issuer.generateToken(authenticatedUser);
        JwtTokenService validator = tokenService(ISSUED_AT, ISSUED_AT.plus(Duration.ofMinutes(121)));

        assertThatThrownBy(() -> validator.validateToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void validateTokenRejectsModifiedToken() {
        JwtTokenService tokenService = tokenService(ISSUED_AT, ISSUED_AT);
        String token = tokenService.generateToken(authenticatedUser);
        String modifiedToken = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> tokenService.validateToken(modifiedToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void validateTokenRejectsInactiveUser() {
        JwtTokenService tokenService = tokenService(ISSUED_AT, ISSUED_AT);
        String token = tokenService.generateToken(authenticatedUser);
        UserDocument inactiveUser = new UserDocument(
                "user-1",
                "Demo Operator",
                "operator@demo.com",
                "hash",
                UserRole.OPERATOR,
                false,
                ISSUED_AT,
                ISSUED_AT);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> tokenService.validateToken(token)).isInstanceOf(JwtException.class);
    }

    private JwtTokenService tokenService(Instant generationTime, Instant validationTime) {
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(SECRET_KEY)
                .algorithm(MacAlgorithm.HS256)
                .build();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(SECRET_KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(Clock.fixed(validationTime, ZoneOffset.UTC));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new UserStatusJwtValidator(userRepository)));

        return new JwtTokenService(
                encoder,
                decoder,
                new JwtProperties("unused-in-unit-test", 120),
                Clock.fixed(generationTime, ZoneOffset.UTC));
    }
}
