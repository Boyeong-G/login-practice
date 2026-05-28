package com.example.demo.auth.controller;

import com.example.demo.auth.dto.*;
import com.example.demo.auth.service.AuthService;
import com.example.demo.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/guest")
    public ResponseEntity<LoginResponse> guestLogin(@RequestBody GuestLoginRequest request) {
        LoginResponse response = this.authService.guestLogin(request.getDeviceId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request, @AuthenticationPrincipal CustomUserDetails currentUser) {
        this.authService.signup(request, currentUser);
        LoginResponse token = this.authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = this.authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse> logout(@AuthenticationPrincipal CustomUserDetails user) {
        this.authService.logout(user.getId());
        SuccessResponse response = SuccessResponse.builder()
                .status(200)
                .code("AUTH_LOGOUT_SUCCESS")
                .message("로그아웃 완료")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/login/success")
    public ResponseEntity<LoginResponse> success(@RequestParam String accessToken, @RequestParam String refreshToken) {
        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        LoginResponse response = this.authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    // 테스트용 (나중에 지우기!!!)
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(SuccessResponse.builder().status(200).code("AUTH_ME_SUCCESS").message(user.getUser().getEmail()).build());
    }
}
