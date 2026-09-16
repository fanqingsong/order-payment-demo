package com.example.orderservice.service;

import com.example.orderservice.dto.RegisterRequest;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.DuplicateEmailException;
import com.example.orderservice.repository.UserRepository;
import com.example.orderservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Test
    void registrationNormalizesEmailAndHashesPasswordWithBcrypt() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService
        );
        when(userRepository.existsByEmail("learner@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(new RegisterRequest(" Learner@Example.com ", "password123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("learner@example.com");
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        AuthService authService = new AuthService(
                userRepository,
                new BCryptPasswordEncoder(),
                authenticationManager,
                jwtService
        );
        when(userRepository.existsByEmail("learner@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("learner@example.com", "password123")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }
}
