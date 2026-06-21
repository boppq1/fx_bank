package com.example.bank.event.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.bank.event.dto.EventDto;
import com.example.bank.event.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventRestController {
	
	private final EventService es;
	
	// 이벤트 참여
    @PostMapping("/join")
    public ResponseEntity<?> joinEvent(@RequestBody Map<String, Long> body) {
        es.joinEvent(body.get("userNo"), body.get("productNo"));
        return ResponseEntity.ok("이벤트 참여 완료");
    }

    // 이벤트 현황 조회
    @GetMapping("/{userNo}/{productNo}")
    public ResponseEntity<?> getEvent(@PathVariable Long userNo,
                                       @PathVariable Long productNo) {
        return ResponseEntity.ok(es.getEvent(userNo, productNo));
    }
    
    // 사진 업로드 & 인증
    public ResponseEntity<?> detect(
            @RequestParam Long userNo,
            @RequestParam Long productNo,
            @RequestParam MultipartFile file) {
        EventDto result = es.uploadAndDetect(userNo, productNo, file);
        return ResponseEntity.ok(result);
    }
}
