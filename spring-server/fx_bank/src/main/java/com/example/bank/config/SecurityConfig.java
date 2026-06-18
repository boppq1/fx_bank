package com.example.bank.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.bank.util.JwtFilter;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("SecurityFilter 동작");
        // 1. 비대면 REST API 서버이므로 불필요한 기본 기능 비활성화
        http.csrf(csrf -> csrf.disable()); // CSRF 공격 방어 비활성화 (토큰 쓰니까 필요 없음)
        http.formLogin(form -> form.disable()); // 기본 제공되는 못생긴 로그인 폼 끄기
        http.httpBasic(basic -> basic.disable()); // 기본 인증 방식 끄기
        
        // 2. 우리는 세션(메모리)을 쓰지 않고 토큰(JWT)과 레디스를 쓸 것이라고 쾅쾅 선언!
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 3. 🚨 URL별 접근 권한 설정 (핵심)
        http.authorizeHttpRequests(auth -> auth
                // 로그인, 재발급, 로그아웃 API와 기본 화면(html, js 등)은 토큰 없이도 무조건 통과 (프리패스)

                .requestMatchers("/a","/", "/login","/register","/reauth", "/api/auth/**","/products", "/product/**","/css/**", "/js/**","/error").permitAll()

                // 2) ⭐ 대출 API 권한 명시 (반드시 anyRequest보다 위에 적어야 함!)
                // .requestMatchers("/api/bank/loan").authenticated() // 또는 권한 적용 시 .hasRole("USER")
                
                // 3) 나머지 모든 요청 차단
                .anyRequest().authenticated()
        );

        // 4. 우리가 만든 커스텀 경비원(JwtFilter)을, 스프링 기본 경비원보다 먼저 입구에 배치
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // FastAPI(OCR) 등 외부 HTTP 호출용 공용 RestTemplate
    @Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
