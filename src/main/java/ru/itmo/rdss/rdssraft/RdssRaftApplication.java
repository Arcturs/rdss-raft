package ru.itmo.rdss.rdssraft;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@RequiredArgsConstructor
public class RdssRaftApplication {

    public static void main(String[] args) {
        SpringApplication.run(RdssRaftApplication.class, args);
    }

}
