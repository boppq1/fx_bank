package com.example.bank.event.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.event.fastapi-url:http://fastapi-bnk:8001}")
    private String eventFastapiUrl;

    public EventDto uploadAndDetect(Long userNo, String letter, MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                eventFastapiUrl + "/detect",
                request,
                Map.class
        );

        Map result = response.getBody();
        if (result == null) {
            throw new RuntimeException("객체탐지 서버 응답이 비어 있습니다.");
        }

        String target = letter.toUpperCase();
        boolean found = Boolean.TRUE.equals(result.get("has_" + target));
        if (!found) {
            throw new RuntimeException(target + " 글자를 찾지 못했습니다. 다시 촬영해주세요.");
        }

        return updateLetter(userNo, target);
    }

    public void joinEvent(Long userNo) {
        EventDto existing = dao.selectEvent(userNo);
        if (existing == null) {
            EventDto event = EventDto.builder()
                    .userNo(userNo)
                    .build();
            dao.insertEvent(event);
        }
    }

    public EventDto updateLetter(Long userNo, String letter) {
        EventDto event = dao.selectEvent(userNo);
        if (event == null) {
            throw new RuntimeException("이벤트 참여 이력이 없습니다.");
        }
        if ("Y".equals(event.getApplied())) {
            throw new RuntimeException("이미 우대금리 쿠폰이 발급된 계정입니다.");
        }

        switch (letter.toUpperCase()) {
            case "B" -> event.setB("Y");
            case "N" -> event.setN("Y");
            case "K" -> event.setK("Y");
            default -> throw new RuntimeException("지원하지 않는 글자입니다.");
        }
        dao.updateLetter(event);

        if ("Y".equals(event.getB()) && "Y".equals(event.getN()) && "Y".equals(event.getK())) {
            dao.insertCoupon(userNo, event.getEventNo());
            dao.updateIsApplied(userNo);
            event.setApplied("Y");
        }
        return event;
    }

    public EventDto getEvent(Long userNo) {
        EventDto event = dao.selectEvent(userNo);
        if (event == null) {
            throw new RuntimeException("이벤트 참여 이력이 없습니다.");
        }
        return event;
    }
}
