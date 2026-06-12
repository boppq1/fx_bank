package com.example.bank.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis에 데이터를 저장하고 만료 시간(유효기간)을 설정합니다.
     * @param key 저장할 키 (예: "RT:qwer")
     * @param value 저장할 값 (예: Refresh Token 문자열)
     * @param durationMillis 만료 시간 (밀리초 단위, 예: 7일에 해당하는 밀리초)
     */
    public void setDataExpire(String key, String value, long durationMillis) {
        stringRedisTemplate.opsForValue().set(key, value, durationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Redis에서 키를 통해 데이터를 조회합니다.
     * @param key 조회할 키
     * @return 저장된 문자열 값 (데이터가 없으면 null 반환)
     */
    public String getData(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * Redis에 저장된 데이터를 삭제합니다. (로그아웃 또는 토큰 강제 만료 시 사용)
     * @param key 삭제할 키
     */
    public void deleteData(String key) {
        stringRedisTemplate.delete(key);
    }
}