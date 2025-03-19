package com.tripsy.repository;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

@Repository
public class TraineeRepository {

    private final WebClient traineeWebClient;

    public TraineeRepository(@Qualifier("apiClient") WebClient traineeWebClient) {
        this.traineeWebClient = traineeWebClient;
    }
	
	public String fetchTraineeByPersonId(String personId) {
		try {
			return traineeWebClient.get().uri("/customers")
					.header("Content-Type", "application/json")
					.retrieve()
					.bodyToMono(String.class)
					.block();
		}
		catch(Exception e){
                   System.out.println("Exception in fetchTraineeByPersonId..."+e);
			throw e;
		}
	}
	
	
}
