package gazpromcps.smg.service;

import gazpromcps.smg.entity.Role;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User save(final User user) {
        return userRepository.save(user);
    }

    public User findById(final long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllByMinRole(final Role min) {
        return userRepository.findAllRoleGreater(min);
    }

    public void promote(final long userId, final Role role) {
        userRepository.promoteToRoleById(userId, role);
    }
}
