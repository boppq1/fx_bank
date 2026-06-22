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
	public EventDto uploadAndDetect(Long userNo, Long productNo, MultipartFile file) {

        // FastAPI로 이미지 전송
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://fastapi-cnn:8000/detect",
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

        return updateLetter(userNo, productNo, detectedLetter);
    }
	
	
	// 이벤트 참여 (적금 가입 시 호출)
    public void joinEvent(Long userNo, Long productNo) {
        EventDto existing = dao.selectEvent(userNo, productNo);
        if (existing == null) {
            EventDto event = EventDto.builder()
                    .userNo(userNo)
                    .productNo(productNo)
                    .build();
            dao.insertEvent(event);
        }
    }
    
 // 글자 인증 업데이트
    public EventDto updateLetter(Long userNo, Long productNo, String letter) {
        EventDto event = dao.selectEvent(userNo, productNo);
        if (event == null) throw new RuntimeException("이벤트 참여 이력이 없습니다.");

        // 해당 글자 Y로 변경
        switch (letter.toUpperCase()) {
            case "B" -> event.setB("Y");
            case "N" -> event.setN("Y");
            case "K" -> event.setK("Y");
        }
        dao.updateLetter(event);

        // B, N, K 모두 Y면 우대금리 적용
        if ("Y".equals(event.getB()) && 
            "Y".equals(event.getN()) && 
            "Y".equals(event.getK())) {
        	dao.updateIsApplied(userNo, productNo);
            event.setApplied("Y");
        }

        return event;
    }

    // 이벤트 현황 조회
    public EventDto getEvent(Long userNo, Long productNo) {
        EventDto event = dao.selectEvent(userNo, productNo);
        if (event == null) throw new RuntimeException("이벤트 참여 이력이 없습니다.");
        return event;
    }
}
