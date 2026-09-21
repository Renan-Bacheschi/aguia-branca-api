package io.github.renanbacheschi.aguiabranca.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.github.renanbacheschi.aguiabranca.user.UserDocument;
import io.github.renanbacheschi.aguiabranca.user.UserRepository;

@Service
public class MongoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MongoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = UserDocument.normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
