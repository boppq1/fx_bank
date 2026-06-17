package com.example.bank.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * [사용 위치: 회원가입 / 재인증 / 주민번호 복호화]
 * 주민등록번호 원문(rrn_enc) 양방향 대칭키 암호화 유틸 (AES-256 / GCM).
 *
 * ⚠️ BCrypt 는 단방향 해시라 복호화가 불가능하므로 주민번호에는 사용하지 않는다.
 *    비밀번호(secu_pw)는 BCrypt, 주민번호는 이 AES 유틸을 사용한다.
 *
 * 비밀키는 코드에 하드코딩하지 않고 application.properties 의 app.crypto.aes-key 로 주입받는다.
 *   - 키는 Base64 로 인코딩된 32바이트(256비트) 값이어야 한다.
 *   - 샘플 키 생성 예시(반드시 운영용은 새로 생성):
 *       openssl rand -base64 32
 *       => 예) "k7Q2mZ8vR4tX1pL9aN6sD3hF0jU5cB2eW8yG4iO1qP0="
 */
@Component
public class EncryptUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;       // GCM 권장 IV 길이(byte)
    private static final int TAG_LENGTH_BIT = 128; // GCM 인증 태그 길이(bit)

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptUtil(@Value("${app.crypto.aes-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("app.crypto.aes-key 는 Base64 인코딩된 32바이트(AES-256) 키여야 합니다.");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 평문 → 암호화. 결과는 [IV(12byte) + 암호문] 을 Base64 로 인코딩한 문자열.
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV 를 앞에 붙여서 함께 저장 (복호화 시 분리해서 사용)
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("주민번호 암호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 암호문(Base64) → 평문 복호화.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(cipherText);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("주민번호 복호화 처리 중 오류가 발생했습니다.", e);
        }
    }
}
