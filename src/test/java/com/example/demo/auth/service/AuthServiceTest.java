package com.example.demo.auth.service;

import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.dto.SignupRequest;
import com.example.demo.auth.enums.LoginType;
import com.example.demo.auth.enums.Role;
import com.example.demo.auth.mapper.UserMapper;
import com.example.demo.exception.BaseException;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.security.userdetails.CustomUserDetails;
import com.example.demo.user.entity.UserEntity;
import com.example.demo.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    // AUTH-01
    @Test
    @DisplayName("새로운 Device ID로 게스트 로그인 시 계정 생성")
    void guestLogin_newDevice_createsGuestAccount() {

        // given
        String deviceId = "device-001";
        String fakeEmail = "guest_" + deviceId + "@temp.local";

        when(userRepository.findByEmail(fakeEmail))
                .thenReturn(Optional.empty());

        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);

                    return UserEntity.builder()
                            .id(1L)
                            .email(user.getEmail())
                            .nickname(user.getNickname())
                            .role(user.getRole())
                            .provider(user.getProvider())
                            .build();
                });

        when(jwtTokenProvider.createAccessToken("1"))
                .thenReturn("access-token");

        when(jwtTokenProvider.createRefreshToken("1"))
                .thenReturn("refresh-token");

        // when
        LoginResponse response = authService.guestLogin(deviceId);

        // then
        ArgumentCaptor<UserEntity> captor =
                ArgumentCaptor.forClass(UserEntity.class);

        verify(userRepository).saveAndFlush(captor.capture());

        UserEntity savedUser = captor.getValue();

        assertEquals(fakeEmail, savedUser.getEmail());
        assertEquals(Role.ROLE_GUEST, savedUser.getRole());
        assertEquals(LoginType.GUEST, savedUser.getProvider());

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        verify(valueOperations).set("auth:refresh:1", "refresh-token");
    }

    // AUTH-02
    @Test
    @DisplayName("동일한 Device ID로 로그인 시 기존 게스트 계정 재사용")
    void guestLogin_existingDevice_reusesGuestAccount() {

        // given
        String deviceId = "device-001";
        String fakeEmail = "guest_" + deviceId + "@temp.local";

        UserEntity existingUser = UserEntity.builder()
                .id(1L)
                .email(fakeEmail)
                .nickname("guest_test")
                .role(Role.ROLE_GUEST)
                .provider(LoginType.GUEST)
                .build();

        when(userRepository.findByEmail(fakeEmail))
                .thenReturn(Optional.of(existingUser));

        when(jwtTokenProvider.createAccessToken("1"))
                .thenReturn("access-token");

        when(jwtTokenProvider.createRefreshToken("1"))
                .thenReturn("refresh-token");

        // when
        LoginResponse response = authService.guestLogin(deviceId);

        // then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        verify(userRepository, never()).saveAndFlush(any(UserEntity.class));

        verify(jwtTokenProvider).createAccessToken("1");
        verify(jwtTokenProvider).createRefreshToken("1");

        verify(valueOperations).set("auth:refresh:1", "refresh-token");
    }

    // AUTH-03
    @Test
    @DisplayName("게스트 계정을 로컬 계정으로 전환 시 기존 ID 유지")
    void signup_guestToPermanent_preservesUserId() {

        // given
        Long userId = 1L;

        UserEntity guestUser = UserEntity.builder()
                .id(userId)
                .email("guest_device-001@temp.local")
                .nickname("guest_test")
                .role(Role.ROLE_GUEST)
                .provider(LoginType.GUEST)
                .build();

        UserEntity permanentUser = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("testUser")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .provider(LoginType.LOCAL)
                .build();

        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .nickname("testUser")
                .password("password123")
                .confirmPassword("password123")
                .build();

        CustomUserDetails currentUser = new CustomUserDetails(guestUser);

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(guestUser));

        when(userMapper.toPermanent(
                eq(userId),
                eq(request),
                eq(Role.ROLE_USER),
                eq(LoginType.LOCAL),
                isNull(),
                eq(passwordEncoder)
        )).thenReturn(permanentUser);

        // when
        authService.signup(request, currentUser);

        // then
        ArgumentCaptor<UserEntity> captor =
                ArgumentCaptor.forClass(UserEntity.class);

        verify(userRepository).saveAndFlush(captor.capture());

        UserEntity savedUser = captor.getValue();

        assertEquals(userId, savedUser.getId());
        assertEquals(Role.ROLE_USER, savedUser.getRole());
        assertEquals(LoginType.LOCAL, savedUser.getProvider());
        assertEquals("test@example.com", savedUser.getEmail());

        verify(userMapper).toPermanent(
                eq(userId),
                eq(request),
                eq(Role.ROLE_USER),
                eq(LoginType.LOCAL),
                isNull(),
                eq(passwordEncoder)
        );
    }

    // AUTH-04
    @Test
    @DisplayName("유효한 Refresh Token으로 새로운 JWT 발급")
    void refresh_validToken_issuesNewTokens() {

        // given
        String refreshToken = "old-refresh-token";
        String userId = "1";

        when(jwtTokenProvider.getUserId(refreshToken))
                .thenReturn(userId);

        when(valueOperations.get("auth:refresh:1"))
                .thenReturn(refreshToken);

        when(jwtTokenProvider.createAccessToken(userId))
                .thenReturn("new-access-token");

        when(jwtTokenProvider.createRefreshToken(userId))
                .thenReturn("new-refresh-token");

        // when
        LoginResponse response = authService.refresh(refreshToken);

        // then
        verify(jwtTokenProvider).validateToken(refreshToken);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        verify(valueOperations).set(
                "auth:refresh:1",
                "new-refresh-token"
        );
    }

    // AUTH-05
    @Test
    @DisplayName("Redis에 저장된 Refresh Token과 다르면 재발급 실패")
    void refresh_mismatchedToken_throwsException() {

        // given
        String refreshToken = "invalid-refresh-token";
        String userId = "1";

        when(jwtTokenProvider.getUserId(refreshToken))
                .thenReturn(userId);

        when(valueOperations.get("auth:refresh:1"))
                .thenReturn("different-refresh-token");

        // when & then
        assertThrows(
                BaseException.class,
                () -> authService.refresh(refreshToken)
        );

        verify(jwtTokenProvider, never()).createAccessToken(anyString());
        verify(jwtTokenProvider, never()).createRefreshToken(anyString());

        verify(valueOperations, never())
                .set(anyString(), anyString());
    }
}