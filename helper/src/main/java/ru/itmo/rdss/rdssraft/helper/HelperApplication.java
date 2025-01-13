package ru.itmo.rdss.rdssraft.helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelperApplication.class, args);
    }

}
