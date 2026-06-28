package com.example.bank.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RestController
public class MbtiApiController {

    private final RestTemplate restTemplate;
    private final String fastApiUrl;

    public MbtiApiController(
            RestTemplate restTemplate,
            @Value("${app.agent.fastapi-url:http://fastapi-agent:8000}") String fastApiUrl
    ) {
        this.restTemplate = restTemplate;
        this.fastApiUrl = fastApiUrl;
    }

    @PostMapping("/api/mbti/analyze-user")
    public ResponseEntity<String> analyzeUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (authorization == null || authorization.isBlank()) {
            return json(401, "로그인이 필요합니다. 다시 로그인해주세요.");
        }

        try {
            return callAnalyzeUser(fastApiUrl, authorization);
        } catch (ResourceAccessException e) {
            return json(503, "금융 MBTI 분석 서버에 연결할 수 없습니다. fastapi-agent 상태를 확인해주세요.");
        } catch (Exception e) {
            return json(500, "금융 MBTI 분석 중 서버 오류가 발생했습니다.");
        }
    }

    private ResponseEntity<String> callAnalyzeUser(String baseUrl, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String targetUrl = baseUrl.replaceAll("/+$", "") + "/analyze-user";

        try {
            return restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
        } catch (ResourceAccessException e) {
            if (!baseUrl.contains("localhost")) {
                return callAnalyzeUser("http://localhost:8000", authorization);
            }
            throw e;
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.trim().startsWith("{")) {
                return ResponseEntity.status(e.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body);
            }
            return json(e.getStatusCode().value(), body == null || body.isBlank()
                    ? "금융 MBTI 분석 서버에서 오류가 발생했습니다."
                    : body);
        }
    }

    private static ResponseEntity<String> json(int status, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"detail\":\"" + escapeJson(detail) + "\"}");
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }
}