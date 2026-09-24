package com.devcommand.devcommand.auth.service;

import com.devcommand.devcommand.auth.dto.AuthResponse;
import com.devcommand.devcommand.auth.dto.LoginRequest;
import com.devcommand.devcommand.auth.dto.RegisterRequest;
import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.security.JwtService;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.user.dto.UserResponse;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(new UserPrincipal(saved));

        return AuthResponse.of(token, UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String token = jwtService.generateToken(new UserPrincipal(user));

        return AuthResponse.of(token, UserResponse.from(user));
    }
}
