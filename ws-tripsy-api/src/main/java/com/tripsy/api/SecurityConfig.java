package com.tripsy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tripsy.api.filter.JwtAuthFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    public JwtAuthFilter jwtAuthFilter;
    
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		//http.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));

		http.csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
		.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/welcome/**").hasAuthority("ROLE_USER")
                  .anyRequest().authenticated() // Protect all other endpoints
              )
		.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); 
		
		return http.build();

	}


}