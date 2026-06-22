package com.example.bank.event.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.bank.event.dao.IEventDao;
import com.example.bank.event.dto.EventDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    
    private final IEventDao dao;
    private final RestTemplate restTemplate;
   
    
    // 사진 업로드 & 글자 인증
    public EventDto uploadAndDetect(Long userNo, String letter, MultipartFile file) {

        // FastAPI로 이미지 전송
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:8000/detect",
            request,
            Map.class
        );

        Map result = response.getBody();

        String target = letter.toUpperCase();
        boolean found = Boolean.TRUE.equals(result.get("has_" + target));
        if (!found) {
            throw new RuntimeException(target + " 글자를 찾지 못했습니다. 다시 촬영해주세요.");
        }

        return updateLetter(userNo, target);
    }
    
    // 이벤트 최초 참여 등록
    public void joinEvent(Long userNo) {
        EventDto existing = dao.selectEvent(userNo);
        if (existing == null) {
            EventDto event = EventDto.builder()
                    .userNo(userNo)
                    .build();
            dao.insertEvent(event);
        }
    }
    
 // 글자 인증 업데이트 및 쿠폰 발급
    public EventDto updateLetter(Long userNo, String letter) {
        EventDto event = dao.selectEvent(userNo);
        if (event == null) throw new RuntimeException("이벤트 참여 이력이 없습니다.");
        if ("Y".equals(event.getApplied())) {
            throw new RuntimeException("이미 우대금리 쿠폰이 발급된 계정입니다.");
        }
        switch (letter.toUpperCase()) {
            case "B" -> event.setB("Y");
            case "N" -> event.setN("Y");
            case "K" -> event.setK("Y");
        }
        dao.updateLetter(event);

        if ("Y".equals(event.getB()) && "Y".equals(event.getN()) && "Y".equals(event.getK())) {
            dao.insertCoupon(userNo, event.getEventNo());   // productNo 안 넘김
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