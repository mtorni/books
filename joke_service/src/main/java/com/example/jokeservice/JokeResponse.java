package com.example.jokeservice;

public class JokeResponse {

    private String content;
    
    private double calculationResult;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public double getCalculationResult() {
		return calculationResult;
	}

	public void setCalculationResult(double calculationResult) {
		this.calculationResult = calculationResult;
	}

}