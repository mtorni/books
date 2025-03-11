package com.tripsy.auth.api.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tripsy.auth.api.config.CustomAuthenticationProvider;
import com.tripsy.auth.api.service.JwtService;
import com.tripsy.auth.api.service.UserInfoService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoService userDetailsService;
    
    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Retrieve the Authorization header
    	
    	System.out.println("uri: " + request.getRequestURI());
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
          //  if (jwtService.validateToken(token, userDetails)) {
            	System.out.println("valid");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    token,
                    userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                customAuthenticationProvider.authenticate(SecurityContextHolder.getContext().getAuthentication());
          //  }
            
            
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}