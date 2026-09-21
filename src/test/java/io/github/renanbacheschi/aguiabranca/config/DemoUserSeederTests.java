package io.github.renanbacheschi.aguiabranca.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import io.github.renanbacheschi.aguiabranca.user.UserRole;
import io.github.renanbacheschi.aguiabranca.user.UserService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DemoUserSeederTests {

    @Test
    void seedCanRunMoreThanOnceUsingIdempotentCreation() {
        UserService userService = mock(UserService.class);
        DemoUserSeeder seeder = new DemoUserSeeder(userService);

        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        verify(userService, times(2)).createUserIfMissing(
                "Demo Operator", "operator@demo.com", "Operator@123", UserRole.OPERATOR);
        verify(userService, times(2)).createUserIfMissing(
                "Demo Manager", "manager@demo.com", "Manager@123", UserRole.MANAGER);
        verify(userService, times(2)).createUserIfMissing(
                "Demo Leader", "leader@demo.com", "Leader@123", UserRole.LEADER);
    }
}
