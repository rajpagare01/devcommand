package com.devcommand.devcommand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DevcommandApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevcommandApplication.class, args);
    }

}
