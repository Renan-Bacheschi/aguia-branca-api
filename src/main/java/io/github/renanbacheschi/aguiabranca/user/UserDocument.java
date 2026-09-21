package io.github.renanbacheschi.aguiabranca.user;

import java.time.Instant;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class UserDocument {

    @Id
    private final String id;

    private final String name;

    @Indexed(unique = true)
    private final String email;

    @JsonIgnore
    private final String passwordHash;

    private final UserRole role;

    private final boolean active;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public UserDocument(String name, String email, String passwordHash, UserRole role, boolean active) {
        this(null, name, email, passwordHash, role, active, null, null);
    }

    @PersistenceCreator
    public UserDocument(
            String id,
            String name,
            String email,
            String passwordHash,
            UserRole role,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
