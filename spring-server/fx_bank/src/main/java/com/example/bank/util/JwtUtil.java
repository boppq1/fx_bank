package com.example.bank.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    // application.properties에 등록된 커스텀 설정값들을 주입받습니다.
    public JwtUtil(@Value("${jwt.secret-key}") String secret,
                   @Value("${jwt.access-expiration}") long accessExpiration,
                   @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * 1. 개발자님의 최신 문법 구조로 Access Token과 Refresh Token 세트를 동시 발급합니다.
     */
    public Map<String, String> generateTokenSet(String userId, String nameKo) {
        long now = System.currentTimeMillis();

        // 30분짜리 Access Token 생성 (유저 이름 포함)
        String accessToken = Jwts.builder()
                .claim("nameKo", nameKo) // claims 먼저 설정
                .subject(userId)         // subject는 반드시 그 다음에
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpiration))
                .signWith(secretKey)     // 최신 규격 서명
                .compact();

        // 7일짜리 Refresh Token 생성 (최소 정보)
        String refreshToken = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshExpiration))
                .signWith(secretKey)
                .compact();

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);

        return tokenMap;
    }

    /**
     * 2. 개발자님이 주신 최신 문법(.verifyWith().getPayload()) 기반의 파싱 메서드입니다.
     * 만료 예외가 발생하더라도 Silent Refresh를 위해 배를 갈라 Claims를 받아오도록 방어합니다.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey) // 최신 규격 검증 방법
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();        // 최신 규격 Payload 추출
        } catch (ExpiredJwtException e) {
            return e.getClaims();         // 만료되어도 내부에 저장된 유저 식별값 확보
        }
    }

    /**
     * 3. 토큰 유효성 검사
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * [신규] 토큰의 남은 유효시간(밀리초) 계산 (최신 규격 적용)
     */
    public long getExpiration(String accessToken) {
        // 이미 만들어두신 parseToken() 메서드를 200% 활용합니다!
        Date expiration = parseToken(accessToken).getExpiration();
        
        // (만료시간 - 현재시간) = 남은 시간
        long now = new Date().getTime();
        return (expiration.getTime() - now); 
    }
    /**
     * 4. Claims에서 유저 ID 추출
     */
    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }
}