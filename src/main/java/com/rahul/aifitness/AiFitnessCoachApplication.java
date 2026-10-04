package com.rahul.aifitness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AiFitnessCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFitnessCoachApplication.class, args);
    }

}
