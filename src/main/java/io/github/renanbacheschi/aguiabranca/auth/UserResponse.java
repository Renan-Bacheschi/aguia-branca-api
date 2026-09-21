package io.github.renanbacheschi.aguiabranca.auth;

import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

public record UserResponse(String id, String name, String email, UserRole role) {

    public static UserResponse from(UserDocument user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public static UserResponse from(AuthenticatedUser user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.role());
    }
}
