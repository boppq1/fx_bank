package com.example.bank.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/analysis")
public class UserAnalysisController {

//    @Value("${fastapi.url}")
//    private String fastapiUrl;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @PostMapping("/user/{userNo}")
//    public ResponseEntity<?> userAnalysis(@PathVariable("userNo") Long userNo) {
//        log.info("[사용자 성향 분석] userNo: {}", userNo);
//        try{
//            String url = fastapiUrl + "/analyze-user/" + userNo;
//            ResponseEntity<String> response = restTemplate.postForEntity(url, this, String.class);
//            log.info("[사용자 성향 분석] 완료 : {}" , response.getBody());
//            return ResponseEntity.ok(response.getBody());
//        }catch(Exception e){
//            log.error("[사용자 성향 분석] 실패 : {}",e.getMessage());
//            return ResponseEntity.internalServerError().body("성향 분석 실패");
//        }
//
//    }


}
