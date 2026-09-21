package io.github.renanbacheschi.aguiabranca.auth;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRole;

public record AuthenticatedUser(
        String id,
        String name,
        String email,
        @JsonIgnore String passwordHash,
        UserRole role,
        boolean active) implements UserDetails {

    public static AuthenticatedUser from(UserDocument user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
