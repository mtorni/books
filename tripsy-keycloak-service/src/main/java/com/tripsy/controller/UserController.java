package com.tripsy.controller;

import java.security.Principal;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

	@GetMapping(path = "/")
	public String index() {
	    return "external";
	}
	    
	@GetMapping(path = "/customers")
	public String customers(Principal principal, Model model) {
	    return "customers";
	}
}
