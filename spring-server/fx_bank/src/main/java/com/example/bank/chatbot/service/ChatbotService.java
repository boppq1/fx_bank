package com.example.bank.chatbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.bank.chatbot.dto.ChatRequest;
import com.example.bank.chatbot.dto.ChatResponse;

@Service
public class ChatbotService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FASTAPI_URL = "http://localhost:8002/api/chat";

    public String ask(String question) {
        ChatRequest request = new ChatRequest();
        request.setQuestion(question);

        ChatResponse response = restTemplate.postForObject(
            FASTAPI_URL,
            request,
            ChatResponse.class
        );

        System.out.println("=== 챗봇 답변: " + response.getAnswer());  // 추가

        return response.getAnswer();
    }
}