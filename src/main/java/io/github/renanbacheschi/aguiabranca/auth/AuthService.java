package io.github.renanbacheschi.aguiabranca.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.error.InvalidAuthenticatedUserException;
import io.github.renanbacheschi.aguiabranca.error.InvalidCredentialsException;
import io.github.renanbacheschi.aguiabranca.user.UserRepository;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    public LoginResponse authenticate(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            String token = jwtTokenService.generateToken(user);

            return new LoginResponse(
                    token,
                    "Bearer",
                    jwtTokenService.getExpirationSeconds(),
                    UserResponse.from(user));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }

    public UserResponse getCurrentUser(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");

        return userRepository.findById(userId)
                .filter(user -> user.isActive() && user.getRole().name().equals(role))
                .map(UserResponse::from)
                .orElseThrow(InvalidAuthenticatedUserException::new);
    }
}
