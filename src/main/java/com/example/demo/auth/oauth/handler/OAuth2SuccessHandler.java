package com.example.demo.auth.oauth.handler;

import com.example.demo.user.entity.UserEntity;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.security.oauth.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${app.oauth2.web-redirect-uri}")
    private String webRedirectUri; // 나중에 지우기

    private static final String REFRESH_PREFIX = "auth:refresh:";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        UserEntity user = customOAuth2User.getUser();

        String accessToken = this.jwtTokenProvider.createAccessToken(user.getId().toString());

        String refreshToken = this.jwtTokenProvider.createRefreshToken(user.getId().toString());

        this.redisTemplate.opsForValue().set(REFRESH_PREFIX + user.getId(), refreshToken);

        String targetUrl = UriComponentsBuilder.fromUriString(webRedirectUri) // redirectUri 이거로 바꾸기
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(
                request,
                response,
                targetUrl
        );
    }
}
