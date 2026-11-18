package com.rural.travel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rural.travel.common.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 游客可访问
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/scenic-spots", "/api/scenic-spots/*",
                    "/api/scenic-spots/*/voice-guide", "/api/scenic-spots/*/ratings",
                    "/api/scenic-spots/*/similar", "/api/recommendations",
                    "/api/categories", "/api/tags").permitAll()
                .requestMatchers("/api/ai/chat", "/api/ai/recognize").permitAll()
                // 方案三：县域文旅知识图谱与智能导览，游客免登录可用
                .requestMatchers(HttpMethod.GET,
                        "/api/guide/counties", "/api/guide/graph", "/api/guide/entities",
                        "/api/guide/entities/*").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/guide/route/plan", "/api/guide/trigger", "/api/guide/ask").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/guide/session/*/touch").permitAll()
                // 管理端
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其余需登录
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(objectMapper.writeValueAsString(
                            Result.fail(401, "登录已过期或尚未登录，请重新登录")));
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(objectMapper.writeValueAsString(
                            Result.fail(403, "没有权限执行此操作")));
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
