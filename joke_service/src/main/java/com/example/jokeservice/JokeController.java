package com.example.jokeservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JokeController {

    private final JokeService jokeService;

    public JokeController(JokeService jokeService) {
        this.jokeService = jokeService;
    }

    @GetMapping("/joke")
    public ResponseEntity<JokeResponse> randomQuote() {
        JokeResponse quote = new JokeResponse();
        quote.setContent(jokeService.randomJoke());
        
        quote.setCalculationResult(performCalculation());
        
        return ResponseEntity.ok(quote);
    }
    
    @GetMapping("/version")
    public ResponseEntity<String> version() {
        String version = "1.0.6";
        
        return ResponseEntity.ok(version);
    }
    
    private double performCalculation() {
        double result = 0;
        for (int i = 0; i < 1000; i++) {
          result += Math.sqrt(i) * Math.sin(i);
        }
        return result;
      }
}
