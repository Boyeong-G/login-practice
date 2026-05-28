package com.example.demo.security.oauth;

import com.example.demo.auth.enums.Role;
import com.example.demo.auth.oauth.factory.OAuth2UserInfoFactory;
import com.example.demo.auth.oauth.info.OAuth2UserInfo;
import com.example.demo.user.entity.UserEntity;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

//        // 1. 현재 HTTP 요청 객체 가져오기
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//
//        // 2. 헤더나 파라미터에서 토큰 추출 (앱 환경에 따라 전달 방식 확인 필요)
//        String header = request.getHeader("Authorization");
//        Long currentUserId = null;
//
//        if (header != null && header.startsWith("Bearer ")) {
//            String token = header.substring(7);
//            try {
//                // 토큰이 유효하다면 userId 추출
//                this.jwtTokenProvider.validateToken(token);
//                currentUserId = Long.valueOf(this.jwtTokenProvider.getUserId(token));
//            } catch (ExpiredJwtException e) {
//                // 토큰 만료는 무시
//            } catch (Exception e) {
//                // 토큰 만료 외의 예외는 무시 (신규 가입으로 처리)
//                currentUserId = null;
//            }
//        }
//
//        String registrationId = userRequest.getClientRegistration().getRegistrationId();
//
//        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
//
//        // 3. 소셜 정보 추출
//        String email = userInfo.getEmail();
//
//        // 4. 전환 또는 조회 로직
//        final Long finalUserId = currentUserId;
//        UserEntity user = this.userRepository.findByEmail(email).orElseGet(() -> createUser(finalUserId, userInfo));

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        UserEntity user = this.userRepository.findByEmail(userInfo.getEmail()).orElseGet(() -> createUser(userInfo));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private UserEntity createUser(OAuth2UserInfo userInfo) {
        UserEntity user = UserEntity.builder()
                .email(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .role(Role.ROLE_USER)
                .build();
        return this.userRepository.save(user);
    }

    private UserEntity createUser(Long id, OAuth2UserInfo userInfo) {
        UserEntity user = UserEntity.builder()
                .id(id)
                .email(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .role(Role.ROLE_USER)
                .build();
        return this.userRepository.save(user);
    }
}
