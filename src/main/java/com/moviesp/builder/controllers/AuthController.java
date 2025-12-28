package com.moviesp.builder.controllers;

import com.moviesp.builder.entities.UserEntity;
import com.moviesp.builder.services.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow nextjs
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<UserEntity> user = userService.login(request.getEmail(), request.getPassword());
        if (user.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "token", user.get().getToken(),
                    "role", user.get().getRole(),
                    "email", user.get().getEmail()));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }
}
