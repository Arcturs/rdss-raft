package ru.itmo.rdss.rdssraft.helper.scheduler;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static org.springframework.http.MediaType.TEXT_PLAIN;
import static ru.itmo.rdss.rdssraft.helper.storage.ServerStorage.SERVERS;

@Slf4j
@Component
@NoArgsConstructor
public class HelperScheduler {

    private static final long MAX_TIME_WITHOUT_UPDATE = 5_000;

    private final RestClient restClient = RestClient.create();

    @Scheduled(fixedRateString = "2000")
    public void heartbeat() {
        for (var server : SERVERS) {
            if (System.currentTimeMillis() - server.getLastUpdated() < 2000) {
                continue;
            }
            try {
                var response = restClient.get()
                        .uri(server.getAddress() + "/api/v1/ping")
                        .accept(TEXT_PLAIN)
                        .retrieve()
                        .body(String.class);
                if (response != null) {
                    server.setLastUpdated(System.currentTimeMillis());
                } else {
                    server.setLastUpdated(0L);
                }
            } catch (Exception e) {
                log.warn("Что-то пошло не так, сервер {} не отвечает", server.getAddress(), e);
                server.setLastUpdated(0L);
            }
        }
    }

    @Scheduled(fixedRateString = "5000")
    public void deleteOldServers() {
        var oldServers = SERVERS.stream()
                .filter(server -> System.currentTimeMillis() - server.getLastUpdated() > MAX_TIME_WITHOUT_UPDATE)
                .toList();
        log.info("Старые сервисы на удаление {}", oldServers);
        if (!oldServers.isEmpty()) {
            SERVERS.removeAll(oldServers);
        }
    }

}
