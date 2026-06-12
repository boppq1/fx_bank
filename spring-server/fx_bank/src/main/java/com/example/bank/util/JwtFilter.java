package com.example.bank.util;

import com.example.bank.util.JwtUtil;
//import com.example.bank.util.RedisUtil; (레디스 유틸 경로에 맞게 추가)
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

 private final JwtUtil jwtUtil;
 private final RedisUtil redisUtil; // 이름은 개발자님 환경에 맞게!

 public JwtFilter(JwtUtil jwtUtil, RedisUtil redisUtil) {
     this.jwtUtil = jwtUtil;
     this.redisUtil = redisUtil;
 }

 @Override
 protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
         throws ServletException, IOException {
	 System.out.println("filter 동작 ");
     // 1. 브라우저가 보낸 헤더에서 Access Token 꺼내기
     String authorizationHeader = request.getHeader("Authorization");
     System.out.println(authorizationHeader);
     if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
         String token = authorizationHeader.substring(7); // "Bearer " 떼어내기

         // 2. 토큰 유효성 검사 (위조되거나 만료되지 않았는가?)
         if (jwtUtil.isValid(token)) {
             
             // 3. 🚨 대망의 블랙리스트 검사 (여기에 있으면 로그아웃된 해커임!)
             String isLogout = redisUtil.getData("BLACKLIST:" + token);
             
             if (isLogout == null) { // 블랙리스트에 없을 때만 통과
                 // 4. 토큰에서 유저 아이디 꺼내기
                 String userId = jwtUtil.getUserId(token);

                 // 5. 서버 전용 VIP 명단(SecurityContextHolder)에 유저 정보 등록!
                 // (지금은 권한이 없으니 빈 리스트(Collections.emptyList())를 넣습니다)
                 UsernamePasswordAuthenticationToken auth = 
                         new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                 SecurityContextHolder.getContext().setAuthentication(auth);
             } else {
                 System.out.println("🚨 블랙리스트에 등록된 토큰 접근 차단!");
             }
         }
     }

     // 6. 경비원의 검사가 끝났으니, 다음 단계(컨트롤러 등)로 넘겨줌
     filterChain.doFilter(request, response);
 }
}