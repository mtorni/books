package com.tripsy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class WsTripsyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WsTripsyApiApplication.class, args);
	}
	
	@Bean
	public RestClient restClient() {
	  return RestClient.create("http://localhost:8090/auth");
	}

}
