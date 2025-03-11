package com.tripsy.auth.api.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripsy.auth.api.entity.AuthRequest;
import com.tripsy.auth.api.entity.UserInfo;
import com.tripsy.auth.api.service.JwtService;
import com.tripsy.auth.api.service.UserInfoApi;
import com.tripsy.auth.api.service.UserInfoService;

@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserInfoService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserInfoService userDetailsService;
    
    @GetMapping("/validate/.well-known/openid-configuration")
    public String validate1() {
    	System.out.println("hello1.1");
    	return "{}";
    }
    
    @GetMapping("/validate")
    public UserInfoApi validate2(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    	token = token.substring(7);
    	System.out.println("token: " + token);
    	String username = jwtService.extractUsername(token);
    	 UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    	 
    	 List<String> roles = new ArrayList<>();
    	 for(GrantedAuthority grantedAutority : userDetails.getAuthorities()) {
    		 roles.add(grantedAutority.getAuthority());
    	 }
    	 
    	 UserInfoApi userInfoApi = new UserInfoApi();
    	 userInfoApi.setEmail(userDetails.getUsername());
    	 userInfoApi.setRoles(roles);
    	 
        return userInfoApi;
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome this endpoint is not secure";
    }

    @PostMapping("/addNewUser")
    public String addNewUser(@RequestBody UserInfo userInfo) {
        return service.addUser(userInfo);
    }

    @GetMapping("/user/userProfile")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String userProfile() {
        return "Welcome to User Profile";
    }

    @GetMapping("/admin/adminProfile")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String adminProfile() {
        return "Welcome to Admin Profile";
    }

    @PostMapping("/generateToken")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
    	System.out.println("gen 1: " + authRequest.toString());
    	try {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );
        System.out.println("what");
        if (authentication.isAuthenticated()) {
        	System.out.println("gen 2");
            return jwtService.generateToken(authRequest.getUsername());
        } else {
        	System.out.println("gen 3");
            throw new UsernameNotFoundException("Invalid user request!");
        }
    	}catch(Exception e) {
    		System.out.println(e.getMessage());
    		e.printStackTrace();
    	}
    	return "adfsad";
    }
}
