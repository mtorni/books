package com.tripsy.api.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	@Autowired
	private RestClient restClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Retrieve the Authorization header
    	
    	System.out.println("uri: " + request.getRequestURI());
    	
    	ResponseEntity<UserInfoApi> s = restClient
    	        .get()
    	        .uri("/validate")
    	        .accept(MediaType.APPLICATION_JSON)
    	        .header("Authorization", request.getHeader("Authorization"))
    	        .retrieve()
    	        .toEntity(UserInfoApi.class);

    	System.out.println("************");
    	System.out.println(s.getBody().getEmail());
    	System.out.println(s.getBody().getName());
    	System.out.println(s.getBody().getPassword());
    	System.out.println(s.getBody().getRoles());
    	System.out.println("************");

    	UserInfoDetails userDetail = new UserInfoDetails(s.getBody());
    
    	
    	System.out.println("valid");
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        		userDetail,
            null,
            userDetail.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
//    	
//    	
//    	
//        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                null,
//                null,
//                grantAuthorities
//            );
//          
//        
//        AbstractAuthenticationToken token = new AbstractAuthenticationToken(List.of());
//        
//            SecurityContextHolder.getContext().setAuthentication(authToken);
//    	
//    	
//        SecurityContextHolder.getContext().setAuthentication(authToken);
    	
    	
    	/*
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // Check if the header starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Extract token
            username = jwtService.extractUsername(token); // Extract username from token
        }

        // If the token is valid and no authentication is set in the context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            System.out.println("userDetails: " + userDetails.getAuthorities());
            System.out.println("token: " + token);
            
            // Validate token and set authentication
            if (jwtService.validateToken(token, userDetails)) {
            	System.out.println("valid");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
*/
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}