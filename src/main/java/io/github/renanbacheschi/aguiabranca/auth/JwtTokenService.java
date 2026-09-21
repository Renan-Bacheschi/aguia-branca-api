package io.github.renanbacheschi.aguiabranca.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            JwtProperties jwtProperties,
            Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.email())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("userId", user.id())
                .claim("role", user.role().name())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt validateToken(String token) {
        return jwtDecoder.decode(token);
    }

    public long getExpirationSeconds() {
        return jwtProperties.expirationMinutes() * 60;
    }
}
