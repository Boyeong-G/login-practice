package com.example.demo.security.jwt;

import com.example.demo.auth.exception.UserErrorCode;
import com.example.demo.user.entity.UserEntity;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.exception.BaseException;
import com.example.demo.security.userdetails.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AntPathMatcher pathMatcher  = new AntPathMatcher();

    private final String[] allowUrl = {
            "/api/auth/guest",
//            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/login/success",
            "/oauth2/**",
            "/login/**"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // allowUrl 배열에 포함된 경로라면 true를 반환하여 필터를 거치지 않게 함
        String path = request.getServletPath();
        return Arrays.stream(allowUrl).anyMatch(url -> pathMatcher.match(url, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String header = request.getHeader("Authorization");

        // 회원가입 요청 + 헤더 존재 X 시 그냥 통과
        if ("/api/auth/signup".equals(uri) && header == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (header != null && header.startsWith("Bearer ")) {
            try {
                // Bearer 제거
                String token = header.substring(7);

                // 토큰 검증 (여기서 예외 발생 가능)
                this.jwtTokenProvider.validateToken(token);

                // userId 추출
                Long userId = Long.valueOf(this.jwtTokenProvider.getUserId(token));

                // 유저 조회
                UserEntity user = this.userRepository.findById(userId).orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

                // 인증 객체 생성
                CustomUserDetails principal = new CustomUserDetails(user);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                // SecurityContext 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 에러 처리
                SecurityContextHolder.clearContext();
                request.setAttribute("exception", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}