package com.example.bank.personal.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

/**
 * [사용 위치: 회원가입 / 재인증 - 신분증 OCR]
 * 프론트가 업로드한 신분증 이미지를 FastAPI(YOLO + CLOVA OCR) 서버로 중계(프록시)한다.
 *
 * 백엔드에서 프록시하는 이유: CLOVA 시크릿 키는 FastAPI 의 .env 에만 존재하며,
 * 프론트에서 직접 호출하면 키/엔드포인트가 노출될 수 있으므로 서버를 거쳐 호출한다.
 *
 * FastAPI 실제 엔드포인트(app.py 확인): POST {base}/ocr/id-card, multipart/form-data, 필드명 "file"
 * 응답 JSON: { success, idType, name, rrnMasked, address, issueDate, detectedLabels[] }
 */
@Service
@RequiredArgsConstructor
public class OcrService {

    private final RestTemplate restTemplate;

    // 예) http://localhost:8000  (application.properties: app.ocr.fastapi-url)
    @Value("${app.ocr.fastapi-url}")
    private String fastapiBaseUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> recognizeIdCard(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("신분증 이미지가 비어 있습니다.");
        }

        String url = fastapiBaseUrl.replaceAll("/+$", "") + "/ocr/id-card";

        try {
            // MultipartFile -> FastAPI 가 파일명/콘텐츠타입을 인식하도록 Resource 로 감싼다.
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "idcard.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            return response.getBody();
        } catch (IOException e) {
            throw new RuntimeException("신분증 이미지 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("OCR 서버 통신에 실패했습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }
}
