package com.seap.smartfinancetracker.auth.service;

import com.seap.smartfinancetracker.auth.dto.AuthResponse;
import com.seap.smartfinancetracker.auth.dto.LoginRequest;
import com.seap.smartfinancetracker.auth.dto.RegisterRequest;
import com.seap.smartfinancetracker.auth.exception.AuthErrorCode;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (request == null || StringUtils.isBlank(request.email()) || StringUtils.isBlank(request.password())) {
            throw new BusinessException(AuthErrorCode.INVALID_INPUT);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .fullName(request.fullName())
                .build();

        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .token(jwtService.generateToken(UserPrincipalMapper.toUserPrincipal(savedUser)))
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {
        if (request == null || StringUtils.isBlank(request.email()) || StringUtils.isBlank(request.password())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        try {
            authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        return AuthResponse.builder()
                .token(jwtService.generateToken(UserPrincipalMapper.toUserPrincipal(user)))
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }
}
