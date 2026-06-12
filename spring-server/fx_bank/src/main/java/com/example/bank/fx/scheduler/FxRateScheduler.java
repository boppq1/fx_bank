package com.example.bank.fx.scheduler;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FxRateScheduler {
	
	private final RestTemplate restTemplate = new RestTemplate();

	@Scheduled(cron = "0 0 0 * * *")
	public void insertRate() {
		
		String url = "";
		String response = restTemplate.getForObject(url, String.class);
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			
			List<Map<String, Object>> list = mapper.readValue(response, List.class);
		}catch (Exception e) {
			
			e.printStackTrace(); // 오류 나면 콘솔에 출력
		}
	}
}
