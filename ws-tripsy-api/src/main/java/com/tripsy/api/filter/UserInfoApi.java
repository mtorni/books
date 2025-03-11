package com.tripsy.api.filter;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoApi {

    private String name;
    private String email;
    private String password;
    private List<String> roles;

}