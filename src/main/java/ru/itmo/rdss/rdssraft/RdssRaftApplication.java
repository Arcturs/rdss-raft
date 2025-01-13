package ru.itmo.rdss.rdssraft;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.itmo.rdss.rdssraft.config.StorageConfiguration;
import ru.itmo.rdss.rdssraft.task.NodeTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EnableScheduling
@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageConfiguration.class)
public class RdssRaftApplication {

    private final NodeTask nodeTask;

    @EventListener(ApplicationReadyEvent.class)
    private void execute() {
        Executors.newSingleThreadExecutor()
                .execute(nodeTask::run);
    }

    public static void main(String[] args) {
        SpringApplication.run(RdssRaftApplication.class, args);
    }

}
