package com.seap.smartfinancetracker.auth.service;

import com.seap.smartfinancetracker.auth.dto.AuthResponse;
import com.seap.smartfinancetracker.auth.dto.LoginRequest;
import com.seap.smartfinancetracker.auth.dto.RegisterRequest;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of authentication service handling user registration and login.
 */
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());

        userRepository.save(user);

        return AuthResponse.builder()
                .token(jwtService.generateToken(UserPrincipalMapper.toUserPrincipal(user)))
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        return AuthResponse.builder()
                .token(jwtService.generateToken(UserPrincipalMapper.toUserPrincipal(user)))
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }
}
