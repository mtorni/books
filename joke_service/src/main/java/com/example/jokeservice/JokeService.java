package com.example.jokeservice;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class JokeService {

    private final String[] quotes = new String[] {
            "Chuck Norris can kill two stones with one bird.",
            "Chuck Norris can divide by zero.",
            "Chuck Norris can slam a revolving door.",
            "Chuck Norris can dribble a bowling ball.",
            "Chuck Norris can do a wheelie on a unicycle.",
            "The dark is afraid of Chuck Norris.",
            "Chuck Norris can cook minute rice in 30 seconds.",
            "Chuck Norris can clap with one hand."
    };

    public String randomJoke() {
        return quotes[new Random().nextInt(quotes.length)];
    }
}