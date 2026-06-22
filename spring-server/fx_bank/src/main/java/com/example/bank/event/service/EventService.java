package com.example.bank.event.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.bank.event.dao.IEventDao;
import com.example.bank.event.dto.EventDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    
    private final IEventDao dao;
    private final RestTemplate restTemplate;
    
    // 사진 업로드 & 글자 인증
    public EventDto uploadAndDetect(Long userNo, Long couponNo, MultipartFile file) {

        // FastAPI로 이미지 전송
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://fastapi-bnk:8000/detect",
            request,
            Map.class
        );

        Map result = response.getBody();

        // 감지된 글자 업데이트
        String detectedLetter = null;
        if (Boolean.TRUE.equals(result.get("has_B"))) detectedLetter = "B";
        else if (Boolean.TRUE.equals(result.get("has_N"))) detectedLetter = "N";
        else if (Boolean.TRUE.equals(result.get("has_K"))) detectedLetter = "K";

        if (detectedLetter == null) {
            throw new RuntimeException("글자를 인식하지 못했습니다. 다시 시도해주세요.");
        }

        return updateLetter(userNo, couponNo, detectedLetter);
    }
    
    // 이벤트 최초 참여 등록
    public void joinEvent(Long userNo, Long couponNo) {
        EventDto existing = dao.selectEvent(userNo);
        if (existing == null) {
            EventDto event = EventDto.builder()
                    .userNo(userNo)
                    .build();
            dao.insertEvent(event);
        }
    }
    
 // 글자 인증 업데이트 및 쿠폰 발급
    public EventDto updateLetter(Long userNo, Long productNo, String letter) {
        EventDto event = dao.selectEvent(userNo);
        if (event == null) throw new RuntimeException("이벤트 참여 이력이 없습니다.");

        if ("Y".equals(event.getApplied())) {
            throw new RuntimeException("이미 우대금리 쿠폰이 발급된 계정입니다.");
        }

        // 해당 글자 Y로 변경
        switch (letter.toUpperCase()) {
            case "B" -> event.setB("Y");
            case "N" -> event.setN("Y");
            case "K" -> event.setK("Y");
        }
        dao.updateLetter(event);

        // B, N, K 모두 Y면 우대금리 쿠폰 발급 및 이벤트 상태 변경
        if ("Y".equals(event.getB()) && 
            "Y".equals(event.getN()) && 
            "Y".equals(event.getK())) {
            
            // 1. 쿠폰 테이블에 발급 데이터 삽입 (event_pk 활용)
            dao.insertCoupon(userNo, event.getEventNo(), productNo);
            
            // 2. 이벤트 테이블의 applied 상태를 'Y'로 변경
            dao.updateIsApplied(userNo); 
            event.setApplied("Y");
        }

        return event;
    }

    // 이벤트 현황 조회
    public EventDto getEvent(Long userNo) {
        EventDto event = dao.selectEvent(userNo);
        if (event == null) throw new RuntimeException("이벤트 참여 이력이 없습니다.");
        return event;
    }
}