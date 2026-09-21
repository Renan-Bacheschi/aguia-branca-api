package io.github.renanbacheschi.aguiabranca.user;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.error.EmailConflictException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDocument createUserIfMissing(String name, String email, String password, UserRole role) {
        String normalizedEmail = UserDocument.normalizeEmail(email);

        return userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> saveNewUser(name, normalizedEmail, password, role));
    }

    private UserDocument saveNewUser(String name, String email, String password, UserRole role) {
        UserDocument user = new UserDocument(name, email, passwordEncoder.encode(password), role, true);

        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new EmailConflictException(email, exception));
        }
    }
}
