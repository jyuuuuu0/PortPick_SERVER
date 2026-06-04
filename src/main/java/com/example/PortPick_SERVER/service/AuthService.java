package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Authenticated email is missing.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }
}
