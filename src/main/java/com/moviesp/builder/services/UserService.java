package com.moviesp.builder.services;

import com.moviesp.builder.entities.UserEntity;
import com.moviesp.builder.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Page<UserEntity> getUsers(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return userRepository.findByEmailContainingIgnoreCase(search, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public Optional<UserEntity> getUser(Long id) {
        return userRepository.findById(id);
    }

    public UserEntity createUser(UserEntity user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        // In a real scenario, password should be hashed here
        return userRepository.save(user);
    }

    public UserEntity updateUser(Long id, UserEntity details) {
        return userRepository.findById(id).map(user -> {
            user.setEmail(details.getEmail());
            user.setRole(details.getRole());
            user.setSubscriptionStatus(details.getSubscriptionStatus());
            // Only update password if provided and not empty
            if (details.getPassword() != null && !details.getPassword().isEmpty()) {
                user.setPassword(details.getPassword());
            }
            return userRepository.save(user);
        }).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void recordLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public Optional<UserEntity> findByToken(String token) {
        return userRepository.findAll().stream()
                .filter(u -> token.equals(u.getToken()))
                .findFirst();
    }

    public Optional<UserEntity> login(String email, String password) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getPassword().equals(password)) { // In real app, verify hash
                String token = java.util.UUID.randomUUID().toString();
                user.setToken(token);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public void seedDefaultAdmin() {
        if (userRepository.count() == 0) {
            UserEntity admin = UserEntity.builder()
                    .email("admin@example.com")
                    // In real app, hash this!
                    .password("admin123")
                    .role("ADMIN")
                    .subscriptionStatus("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user.");
        }
    }
}
