package io.github.renanbacheschi.aguiabranca.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.renanbacheschi.aguiabranca.user.UserRole;
import io.github.renanbacheschi.aguiabranca.user.UserService;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoUserSeeder implements ApplicationRunner {

    private final UserService userService;

    public DemoUserSeeder(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        userService.createUserIfMissing(
                "Demo Operator", "operator@demo.com", "Operator@123", UserRole.OPERATOR);
        userService.createUserIfMissing(
                "Demo Manager", "manager@demo.com", "Manager@123", UserRole.MANAGER);
        userService.createUserIfMissing(
                "Demo Leader", "leader@demo.com", "Leader@123", UserRole.LEADER);
    }
}
