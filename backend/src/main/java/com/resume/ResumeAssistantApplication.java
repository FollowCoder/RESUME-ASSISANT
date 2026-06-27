package com.resume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResumeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAssistantApplication.class, args);
    }
}
