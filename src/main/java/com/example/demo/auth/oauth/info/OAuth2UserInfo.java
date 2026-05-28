package com.example.demo.auth.oauth.info;

import com.example.demo.auth.enums.LoginType;

public interface OAuth2UserInfo {
    String getEmail();
    String getNickname();
    String getProviderId();
    LoginType getProvider();
}
