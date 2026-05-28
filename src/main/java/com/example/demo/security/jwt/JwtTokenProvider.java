package com.example.demo.security.jwt;

import com.example.demo.exception.BaseException;
import com.example.demo.security.exception.JwtErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long access;

    @Value("${jwt.refresh-expiration}")
    private Long refresh;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(this.secret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String createAccessToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration((new Date(System.currentTimeMillis() + access)))
                .signWith(getSecretKey())
                .compact();
    }

    public String createRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refresh))
                .signWith(getSecretKey())
                .compact();
    }

    public String getUserId(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public void validateToken(String token){
        try {
            Claims claims = parseToken(token);

            if (!claims.getIssuer().equals(issuer)) {
                throw new BaseException(JwtErrorCode.JWT_INVALID_TOKEN);
            }
        } catch (ExpiredJwtException e) {
            throw new BaseException(JwtErrorCode.JWT_EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new BaseException(JwtErrorCode.JWT_UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new BaseException(JwtErrorCode.JWT_MALFORMED_TOKEN);
        } catch (Exception e) {
            throw new BaseException(JwtErrorCode.JWT_INVALID_TOKEN);
        }
    }
}