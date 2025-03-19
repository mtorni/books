package com.tripsy.controller;

import java.security.Principal;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripsy.repository.TraineeRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
	
	private final TraineeRepository traineeRepository;

	@GetMapping(path = "/")
	public String index() {
	    return "external";
	}
	    
	@GetMapping(path = "/customers")
	public String customers(Principal principal, Model model) {
	    return "customers";
	}
	
	@GetMapping(path = "/users")
	public String users(Principal principal, Model model) {
		System.out.println("usersXX..."+principal.getName());
		String s = traineeRepository.fetchTraineeByPersonId("1");
		System.out.println("usersYY..."+s);
	    return "users";
	}
}
