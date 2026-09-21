package io.github.renanbacheschi.aguiabranca.user;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTests {

    @Test
    void createUserIfMissingIsIdempotentAndStoresOnlyBcryptHash() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserService userService = new UserService(userRepository, passwordEncoder);
        AtomicReference<UserDocument> storedUser = new AtomicReference<>();

        when(userRepository.findByEmail("operator@demo.com"))
                .thenAnswer(invocation -> Optional.ofNullable(storedUser.get()));
        when(userRepository.save(any(UserDocument.class))).thenAnswer(invocation -> {
            UserDocument user = invocation.getArgument(0);
            storedUser.set(user);
            return user;
        });

        UserDocument first = userService.createUserIfMissing(
                "Demo Operator", " OPERATOR@DEMO.COM ", "Operator@123", UserRole.OPERATOR);
        UserDocument second = userService.createUserIfMissing(
                "Demo Operator", "operator@demo.com", "Operator@123", UserRole.OPERATOR);

        assertThat(second).isSameAs(first);
        assertThat(first.getEmail()).isEqualTo("operator@demo.com");
        assertThat(first.getPasswordHash()).isNotEqualTo("Operator@123");
        assertThat(passwordEncoder.matches("Operator@123", first.getPasswordHash())).isTrue();
        verify(userRepository, times(1)).save(any(UserDocument.class));
    }
}
