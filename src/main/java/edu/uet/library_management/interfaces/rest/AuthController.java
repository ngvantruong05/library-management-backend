package edu.uet.library_management.interfaces.rest;

import edu.uet.library_management.domain.dto.*;
import edu.uet.library_management.domain.enums.Role;
import edu.uet.library_management.domain.model.User;
import edu.uet.library_management.infrastructure.persistence.UserRepository;
import edu.uet.library_management.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${access.token.expiration.time:90000}")
    private long accessTokenExpiration;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use"));
        }

        Role role = Role.USER;
        if (userRepository.count() == 0) {
            role = Role.ADMIN;
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .birthday(request.getBirthday())
                .phoneNumber(request.getPhoneNumber())
                .photoUrl(request.getPhotoUrl())
                .role(role)
                .disabled(false)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiration)
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiration)
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .role(user.getRole())
                    .build();

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired refresh token"));
        }

        try {
            String email = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            User user = (User) userDetails;

            String newAccessToken = jwtService.generateAccessToken(userDetails);

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiration)
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .role(user.getRole())
                    .build();

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Failed to refresh token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    UserDto userDto = UserDto.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .displayName(user.getDisplayName())
                            .birthday(user.getBirthday())
                            .phoneNumber(user.getPhoneNumber())
                            .photoUrl(user.getPhotoUrl())
                            .role(user.getRole())
                            .createdAt(user.getCreatedAt())
                            .build();
                    return ResponseEntity.ok(userDto);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UserUpdateRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        user.setDisplayName(request.getDisplayName());
        user.setBirthday(request.getBirthday());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPhotoUrl(request.getPhotoUrl());
        
        userRepository.save(user);
        
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .birthday(user.getBirthday())
                .phoneNumber(user.getPhoneNumber())
                .photoUrl(user.getPhotoUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
                
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(@RequestBody PasswordUpdateRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu cũ không chính xác"));
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "Cập nhật mật khẩu thành công"));
    }

    @Data
    public static class PasswordUpdateRequest {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
