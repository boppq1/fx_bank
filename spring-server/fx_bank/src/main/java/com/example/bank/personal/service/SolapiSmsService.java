package com.example.bank.personal.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.bank.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolapiSmsService {

    private static final String API_URL = "https://api.solapi.com/messages/v4/send";
    private static final String VERIFY_KEY_PREFIX = "PHONE_VERIFY:";
    private static final long VERIFY_EXPIRE_MILLIS = 3 * 60 * 1000L;
    private static final DateTimeFormatter SOLAPI_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final RestTemplate restTemplate;
    private final RedisUtil redisUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.solapi.api-key:}")
    private String apiKey;

    @Value("${app.solapi.api-secret:}")
    private String apiSecret;

    @Value("${app.solapi.from:}")
    private String from;

    public void sendVerificationCode(String phone) {
        validateConfiguration();
        String normalizedPhone = normalizePhone(phone);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String date = SOLAPI_DATE_FORMAT.format(Instant.now());
        String salt = UUID.randomUUID().toString().replace("-", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "HMAC-SHA256 apiKey=" + apiKey
                + ", date=" + date
                + ", salt=" + salt
                + ", signature=" + makeSignature(date, salt));

        Map<String, Object> body = Map.of(
                "message", Map.of(
                        "to", normalizedPhone,
                        "from", normalizePhone(from),
                        "text", "[부산은행] 인증번호는 " + code + " 입니다."
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, new HttpEntity<>(body, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("솔라피 문자 발송에 실패했습니다.");
            }
            redisUtil.setDataExpire(VERIFY_KEY_PREFIX + normalizedPhone, code, VERIFY_EXPIRE_MILLIS);
        } catch (RuntimeException e) {
            throw new IllegalStateException("솔라피 문자 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public void verifyCode(String phone, String code) {
        String normalizedPhone = normalizePhone(phone);
        String savedCode = redisUtil.getData(VERIFY_KEY_PREFIX + normalizedPhone);
        if (savedCode == null || !savedCode.equals(code == null ? "" : code.trim())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않거나 만료되었습니다.");
        }
        redisUtil.deleteData(VERIFY_KEY_PREFIX + normalizedPhone);
    }

    private String makeSignature(String date, String salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(signature.length * 2);
            for (byte value : signature) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("솔라피 인증 서명 생성에 실패했습니다.", e);
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private void validateConfiguration() {
        if (apiKey.isBlank() || apiSecret.isBlank() || from.isBlank()) {
            throw new IllegalStateException("솔라피 환경변수 설정이 필요합니다.");
        }
    }
}
