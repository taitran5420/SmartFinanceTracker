package com.seap.smartfinancetracker.auth.service;

import com.seap.smartfinancetracker.auth.dto.AuthResponse;
import com.seap.smartfinancetracker.auth.dto.LoginRequest;
import com.seap.smartfinancetracker.auth.dto.RegisterRequest;
import com.seap.smartfinancetracker.auth.exception.AuthErrorCode;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceImplTest {

    //<editor-fold desc="Setup & Configurations">
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserPrincipalMapper userPrincipalMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String TOKEN = "generated.jwt.token";
    private static final long EXPIRES_IN = 3600L;
    //</editor-fold>

    //<editor-fold desc="Test register">
    @Test
    @DisplayName("Should register a new user, encode the password, assign USER role and return tokens")
    void register_ShouldPersistUserAndReturnAuthResponse() {
        // 1. Arrange
        RegisterRequest request = new RegisterRequest("jane@example.com", "plainPassword", "Jane Doe");
        String encodedPassword = "encoded-password";

        User savedUser = Instancio.create(User.class);
        UserPrincipal principal = Instancio.create(UserPrincipal.class);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userPrincipalMapper.toUserPrincipal(savedUser)).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn(TOKEN);
        when(jwtService.getExpirationTime()).thenReturn(EXPIRES_IN);

        // 2. Act
        AuthResponse response = authService.register(request);

        // 3. Assert
        assertNotNull(response);
        assertEquals(TOKEN, response.token());
        assertEquals(EXPIRES_IN, response.expiresIn());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();

        assertEquals(request.email(), persisted.getEmail());
        assertEquals(encodedPassword, persisted.getPassword(), "Raw password must never be stored");
        assertEquals(request.fullName(), persisted.getFullName());
        assertEquals(Role.USER, persisted.getRole(), "New users must default to the USER role");
    }

    @Test
    @DisplayName("Should reject registration when the email already exists")
    void register_ShouldThrow_WhenEmailAlreadyExists() {
        // 1. Arrange
        RegisterRequest request = new RegisterRequest("taken@example.com", "plainPassword", "John Doe");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when the request is null")
    void register_ShouldThrow_WhenRequestIsNull() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(null)
        );

        assertEquals(AuthErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Should reject registration when the email is blank")
    void register_ShouldThrow_WhenEmailIsBlank() {
        RegisterRequest request = new RegisterRequest("   ", "plainPassword", "Jane Doe");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertEquals(AuthErrorCode.INVALID_INPUT, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when the password is blank")
    void register_ShouldThrow_WhenPasswordIsBlank() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "  ", "Jane Doe");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertEquals(AuthErrorCode.INVALID_INPUT, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }
    //</editor-fold>

    //<editor-fold desc="Test authenticate">
    @Test
    @DisplayName("Should authenticate valid credentials and return tokens")
    void authenticate_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        // 1. Arrange
        LoginRequest request = new LoginRequest("jane@example.com", "plainPassword");

        User user = Instancio.create(User.class);
        UserPrincipal principal = Instancio.create(UserPrincipal.class);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(userPrincipalMapper.toUserPrincipal(user)).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn(TOKEN);
        when(jwtService.getExpirationTime()).thenReturn(EXPIRES_IN);

        // 2. Act
        AuthResponse response = authService.authenticate(request);

        // 3. Assert
        assertNotNull(response);
        assertEquals(TOKEN, response.token());
        assertEquals(EXPIRES_IN, response.expiresIn());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
    }

    @Test
    @DisplayName("Should reject login when the authentication manager rejects the credentials")
    void authenticate_ShouldThrowInvalidCredentials_WhenAuthenticationFails() {
        // 1. Arrange
        LoginRequest request = new LoginRequest("jane@example.com", "wrongPassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Should reject login when the user disappears after authentication")
    void authenticate_ShouldThrowInvalidCredentials_WhenUserNotFound() {
        // 1. Arrange
        LoginRequest request = new LoginRequest("ghost@example.com", "plainPassword");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verify(jwtService, never()).generateToken(any(UserPrincipal.class));
    }

    @Test
    @DisplayName("Should reject login when the request is null")
    void authenticate_ShouldThrow_WhenRequestIsNull() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.authenticate(null)
        );

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verifyNoInteractions(authenticationManager, userRepository, jwtService);
    }

    @Test
    @DisplayName("Should reject login when credentials are blank")
    void authenticate_ShouldThrow_WhenCredentialsAreBlank() {
        LoginRequest request = new LoginRequest("", "");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verifyNoInteractions(authenticationManager, userRepository, jwtService);
    }
    //</editor-fold>
}
