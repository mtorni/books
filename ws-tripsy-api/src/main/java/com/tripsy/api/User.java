package com.tripsy.api;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class User implements UserDetails {

	@Override
	public List<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}
    // Your user properties
    // implement methods



	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return null;
	}
}
