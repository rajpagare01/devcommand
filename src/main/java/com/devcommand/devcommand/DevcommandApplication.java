package com.devcommand.devcommand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevcommandApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevcommandApplication.class, args);
    }

}
