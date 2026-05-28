package com.example.demo.auth.mapper;

import com.example.demo.auth.dto.SignupRequest;
import com.example.demo.auth.enums.LoginType;
import com.example.demo.auth.enums.Role;
import com.example.demo.user.entity.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    // User: DTO -> Entity
    public UserEntity toEntity(SignupRequest request, Role role, LoginType provider, String providerId, PasswordEncoder passwordEncoder) {
        return UserEntity.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    public UserEntity toPermanent(Long id, SignupRequest request, Role role, LoginType provider, String providerId, PasswordEncoder passwordEncoder) {
        return UserEntity.builder()
                .id(id)
                .email(request.getEmail())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(provider)
                .providerId(providerId)
                .build();
    }
}
