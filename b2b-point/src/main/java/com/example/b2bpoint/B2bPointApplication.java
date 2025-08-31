package com.example.b2bpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class B2bPointApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2bPointApplication.class, args);
    }

}
