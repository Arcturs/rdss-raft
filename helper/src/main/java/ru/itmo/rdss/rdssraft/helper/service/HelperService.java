package ru.itmo.rdss.rdssraft.helper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.rdss.rdssraft.helper.entity.Server;

import java.util.UUID;
import java.util.stream.Collectors;

import static ru.itmo.rdss.rdssraft.helper.storage.ServerStorage.MASTER_ADDRESS;
import static ru.itmo.rdss.rdssraft.helper.storage.ServerStorage.SERVERS;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelperService {

    public String getServers() {
        return SERVERS.stream()
                .map(Server::getAddress)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public String getLeader() {
        return MASTER_ADDRESS;
    }

    public void setLeader(String leader) {
        MASTER_ADDRESS = leader;
        log.info("Записан лидер с адресом {}", leader);
        for (var server : SERVERS) {
            if (server.getAddress().equals(leader)) {
                server.setLastUpdated(System.currentTimeMillis());
                return;
            }
        }
    }

    public void addServer(String server) {
        var parsedServer = server.split(" ");
        SERVERS.add(new Server()
                .setId(UUID.fromString(parsedServer[0]))
                .setAddress(parsedServer[1])
                .setLastUpdated(System.currentTimeMillis()));
        log.info("Зарегистрирована нода с адресом {}", parsedServer[1]);
    }

}
