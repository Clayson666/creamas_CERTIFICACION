package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 1. DESHABILITAR CSRF (Esto suele arreglar el 403 en local)
                                .csrf(csrf -> csrf.disable())

                                .authorizeHttpRequests((requests) -> requests
                                                // 2. PERMITIR RECURSOS ESTÁTICOS Y LA RUTA DE ERROR
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**",
                                                                "/error")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .formLogin((form) -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/upload", true)
                                                .permitAll())
                                .logout((logout) -> logout
                                                .permitAll());

                return http.build();
        }
}