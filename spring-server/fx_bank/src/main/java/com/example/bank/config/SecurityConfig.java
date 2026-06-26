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
        System.out.println("SecurityFilter ?숈옉");
        // 1. 鍮꾨?硫?REST API ?쒕쾭?대?濡?遺덊븘?뷀븳 湲곕낯 湲곕뒫 鍮꾪솢?깊솕
        http.csrf(csrf -> csrf.disable()); // CSRF 怨듦꺽 諛⑹뼱 鍮꾪솢?깊솕 (?좏겙 ?곕땲源??꾩슂 ?놁쓬)
        http.formLogin(form -> form.disable()); // 湲곕낯 ?쒓났?섎뒗 紐살깮湲?濡쒓렇?????꾧린
        http.httpBasic(basic -> basic.disable()); // 기본 인증 방식 끄기
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        
        // 2. ?곕━???몄뀡(硫붾え由????곗? ?딄퀬 ?좏겙(JWT)怨??덈뵒?ㅻ? ??寃껋씠?쇨퀬 苡낆푷 ?좎뼵!
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 3. ?슚 URL蹂??묎렐 沅뚰븳 ?ㅼ젙 (?듭떖)
        http.authorizeHttpRequests(auth -> auth
                // 濡쒓렇?? ?щ컻湲? 濡쒓렇?꾩썐 API? 湲곕낯 ?붾㈃(html, js ??? ?좏겙 ?놁씠??臾댁“嫄??듦낵 (?꾨━?⑥뒪)
        		.requestMatchers("/a","/", "/login","/register","/reauth", "/api/auth/**","/products", "/product/**","/fx/**", "/api/fx/**","/css/**", "/js/**", "/admin/**", "/error", "/event/**", "/event-status", "/chatbot/**", "/mbti", "/api/product/terms/*/pdf", "/api/product/join/*/terms/*/pdf", "/api/product/join/*/terms/*/pdf/page/*").permitAll()

                // 2) 狩??異?API 沅뚰븳 紐낆떆 (諛섎뱶??anyRequest蹂대떎 ?꾩뿉 ?곸뼱????)
                // .requestMatchers("/api/bank/loan").authenticated() // ?먮뒗 沅뚰븳 ?곸슜 ??.hasRole("USER")
                
                // 3) ?섎㉧吏 紐⑤뱺 ?붿껌 李⑤떒
                .anyRequest().authenticated()
        );

        // 4. ?곕━媛 留뚮뱺 而ㅼ뒪? 寃쎈퉬??JwtFilter)?? ?ㅽ봽留?湲곕낯 寃쎈퉬?먮낫??癒쇱? ?낃뎄??諛곗튂
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}


