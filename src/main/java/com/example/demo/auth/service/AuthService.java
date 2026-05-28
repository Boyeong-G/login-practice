package com.example.demo.auth.service;

import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.dto.SignupRequest;
import com.example.demo.auth.enums.LoginType;
import com.example.demo.auth.enums.Role;
import com.example.demo.auth.exception.AuthErrorCode;
import com.example.demo.auth.exception.UserErrorCode;
import com.example.demo.auth.mapper.UserMapper;
import com.example.demo.user.entity.UserEntity;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.exception.BaseException;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.security.userdetails.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_PREFIX = "auth:refresh:";

    @Transactional
    public LoginResponse guestLogin(String deviceId) {
        // deviceId를 이용한 가짜 이메일 생성
        String fakeEmail = "guest_" + deviceId + "@temp.local";

        // deviceId로 기존 게스트 유저 조회, 없으면 새로 생성
        UserEntity user = this.userRepository.findByEmail(fakeEmail).orElseGet(() -> {
                    // 새 게스트 유저를 위한 고유 값 생성 (닉네임용)
                    String nickname = "guest_" + UUID.randomUUID().toString().substring(0, 8);
                    return createUser(fakeEmail, nickname, Role.ROLE_GUEST, LoginType.GUEST);
                });

        String accessToken = this.jwtTokenProvider.createAccessToken(user.getId().toString());
        String refreshToken = this.jwtTokenProvider.createRefreshToken(user.getId().toString());

        // Redis 저장
        this.redisTemplate.opsForValue().set(REFRESH_PREFIX + user.getId(), refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void signup(SignupRequest request, CustomUserDetails currentUser) {
        // password와 email 유효성 검사
        validateSignup(request);

        // 게스트 -> 회원 전환
        if (currentUser != null) {
            UserEntity user = this.userRepository.findById(currentUser.getId()).orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
            user = this.userMapper.toPermanent(user.getId(), request, Role.ROLE_USER, LoginType.LOCAL, null, passwordEncoder);
            this.userRepository.saveAndFlush(user);
            return;
        }

        // 일반 회원가입
        UserEntity user = this.userMapper.toEntity(request, Role.ROLE_USER, LoginType.LOCAL, null, passwordEncoder);
        this.userRepository.saveAndFlush(user);
    }

    public LoginResponse login(String email, String password) {
        UserEntity user = this.userRepository.findByEmail(email).orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        if (!this.passwordEncoder.matches(password, user.getPassword())) {
            throw new BaseException(AuthErrorCode.AUTH_INVALID_PASSWORD);
        }

        String accessToken = this.jwtTokenProvider.createAccessToken(user.getId().toString());
        String refreshToken = this.jwtTokenProvider.createRefreshToken(user.getId().toString());

        // Redis 저장
        this.redisTemplate.opsForValue().set(REFRESH_PREFIX + user.getId(), refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(Long userId) {
        this.redisTemplate.delete(REFRESH_PREFIX + userId);
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null) {
            throw new BaseException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        // JWT 검증 (예외 기반)
        try {
            this.jwtTokenProvider.validateToken(refreshToken);
        } catch (BaseException e) {
            throw new BaseException(AuthErrorCode.AUTH_LOGIN_REQUIRED);
        }

        String userId = this.jwtTokenProvider.getUserId(refreshToken);
        String redisRefresh = this.redisTemplate.opsForValue().get(REFRESH_PREFIX + userId);

        if (!refreshToken.equals(redisRefresh)) {
            throw new BaseException(AuthErrorCode.AUTH_REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = this.jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = this.jwtTokenProvider.createRefreshToken(userId);

        // Redis 저장
        this.redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, newRefreshToken);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void validateSignup(SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(AuthErrorCode.AUTH_PASSWORD_MISMATCH);
        }
        if (this.userRepository.existsByEmail(request.getEmail())) {
            throw new BaseException(UserErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
    }

    private UserEntity createUser(String fakeEmail, String nickname, Role role, LoginType type) {
        UserEntity user = UserEntity.builder()
                .email(fakeEmail)
                .nickname(nickname)
                .role(role)
                .provider(type)
                .build();
        return userRepository.saveAndFlush(user);
    }
}