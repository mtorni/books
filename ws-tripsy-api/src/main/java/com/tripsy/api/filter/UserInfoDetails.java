package com.tripsy.api.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserInfoDetails implements UserDetails {

    private String username; // Changed from 'name' to 'username' for clarity
    private String password;
    private List<GrantedAuthority> authorities;

    public UserInfoDetails(UserInfo userInfo) {
        this.username = userInfo.getEmail(); // Assuming 'name' is used as 'username'
        this.password = userInfo.getPassword();
        this.authorities = List.of(userInfo.getRoles().split(","))
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
    
    public UserInfoDetails(UserInfoApi userInfo) {
        this.username = userInfo.getEmail(); // Assuming 'name' is used as 'username'
        this.password = userInfo.getPassword();
        
        List<GrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
        for(String role : userInfo.getRoles()) {
        	simpleGrantedAuthorities.add(new SimpleGrantedAuthority(role));
        }
        
        this.authorities = simpleGrantedAuthorities;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Implement your logic if you need this
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Implement your logic if you need this
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Implement your logic if you need this
    }

    @Override
    public boolean isEnabled() {
        return true; // Implement your logic if you need this
    }
}
