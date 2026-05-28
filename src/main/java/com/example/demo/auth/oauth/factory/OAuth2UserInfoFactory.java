package com.example.demo.auth.oauth.factory;

import com.example.demo.auth.oauth.info.KakaoOAuth2UserInfo;
import com.example.demo.auth.oauth.info.OAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toUpperCase()) {
            case "KAKAO" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }
}