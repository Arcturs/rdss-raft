package ru.itmo.rdss.rdssraft.task;

import jakarta.annotation.PreDestroy;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.itmo.rdss.rdssraft.entity.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static ru.itmo.rdss.rdssraft.dictionary.NodeState.CANDIDATE;
import static ru.itmo.rdss.rdssraft.dictionary.NodeState.FOLLOWER;
import static ru.itmo.rdss.rdssraft.dictionary.NodeState.LEADER;

@Slf4j
@Service
@NoArgsConstructor
public class NodeTask {

    private final RestClient restClient = RestClient.create();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Value("${storage.port}")
    private String port;

    public void run() {
        var node = Node.getInstance();
        log.info("Запуск ноды с ИД {}", node.getId());
        restClient.post()
                .uri("http://host.docker.internal:8080/api/v1/server")
                .contentType(TEXT_PLAIN)
                .body(node.getId() + " " + "http://host.docker.internal:" + port)
                .retrieve()
                .toBodilessEntity();

        while (true) {
            switch (node.getState()) {
                case FOLLOWER:
                    follower();
                    break;
                case CANDIDATE:
                    election();
                    break;
                case LEADER:
                    sendHeartbeat();
                    break;
            }
        }
    }

    private void follower() {
        var node = Node.getInstance();
        if (node.getMasterAddress() == null) {
            var leaderAddress = restClient.get()
                    .uri("http://host.docker.internal:8080/api/v1/leader")
                    .accept(TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);
            if (StringUtils.isBlank(leaderAddress)) {
                log.info("Мастера нет в кластере, нода становится кандидатом");
                node.setState(CANDIDATE);
                return;
            }
            node.setMasterAddress(leaderAddress);
            node.setMasterLastUpdated(System.currentTimeMillis());
            log.info("Получен мастер: {}", leaderAddress);
            return;
        }
        var electionTimeout = Node.getElectionTimeout();
        try {
            Thread.sleep(electionTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Произошла ошибка", e);
            return;
        }
        if (System.currentTimeMillis() - node.getMasterLastUpdated() > electionTimeout) {
            node.setState(CANDIDATE);
            node.setMasterAddress(null);
            log.info("Мастер не отвечает, нода становится кандидатом");
        }
    }

    private void election() {
        log.info("Начало голосования!");
        var serversAddresses = getServersAddresses();
        log.info("Узлы: {}", serversAddresses);
        var votes = 1;
        var node = Node.getInstance();
        node.setHasVotedFor(node.getId());

        List<Callable<Pair<String, ResponseEntity<String>>>> tasks = getTasks(
                serversAddresses,
                address -> (ResponseEntity<String>) safeApiCall(restClient.get()
                        .uri(address + "/api/v1/" + node.getId() + "/vote")
                        .accept(TEXT_PLAIN)
                        .retrieve()
                        .toEntity(String.class)));
        try {
            List<Future<Pair<String, ResponseEntity<String>>>> results = executor.invokeAll(tasks, 1, SECONDS);
            for (var r : results) {
                var result = r.get();
                if (result == null) {
                    log.warn("Запрос ноды завершился по таймауту");
                    continue;
                }
                var resultResponse = result.getRight();
                var address = result.getLeft();

                var serverTerm = Integer.getInteger(resultResponse.getBody());
                if (serverTerm != null && serverTerm > node.getCurrentTerm()) {
                    log.info("Нода с адресом {} имеет больший срок, голосование завершается", address);
                    node.setCurrentTerm(serverTerm);
                    node.setHasVotedFor(null);
                    node.setState(FOLLOWER);
                    return;
                }
                if (resultResponse.getStatusCode().is2xxSuccessful()) {
                    votes++;
                } else {
                    log.info("Нода с адресом {} уже проголосовала за другого кандидата", address);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Что-то пошло не так", e);
        }

        log.info("Получено голосов {}", votes);
        if (votes > serversAddresses.size() / 2) {
            node.setState(LEADER);
            node.setCurrentTerm(node.getCurrentTerm() + 1);
            restClient.post()
                    .uri("http://host.docker.internal:8080/api/v1/leader")
                    .contentType(TEXT_PLAIN)
                    .body("http://host.docker.internal:" + port)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Нода становится лидером!");
        } else {
            node.setState(FOLLOWER);
            log.info("Голоса не собраны, голосование не состоялось");
        }
        node.setHasVotedFor(null);
    }

    private List<String> getServersAddresses() {
        var response = restClient.get()
                .uri("http://host.docker.internal:8080/api/v1/servers")
                .accept(TEXT_PLAIN)
                .retrieve()
                .body(String.class);
        return Arrays.stream(response.replace("[", "")
                        .replace("]", "")
                        .split(", "))
                .toList();
    }

    private <V> List<Callable<Pair<String, ResponseEntity<V>>>> getTasks(
            List<String> serverAddresses,
            Function<String, ResponseEntity<V>> function) {

        List<Callable<Pair<String, ResponseEntity<V>>>> tasks = new ArrayList<>();
        for (var address : serverAddresses) {
            if (address.equals("http://host.docker.internal:" + port)) {
                continue;
            }
            tasks.add(() -> Pair.of(address, function.apply(address)));
        }
        return tasks;
    }

    private void sendHeartbeat() {
        try {
            Thread.sleep(Node.getInstance().getHeartbeatTimeout());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Произошла ошибка", e);
            return;
        }
        var serversAddresses = getServersAddresses();
        log.info("Узлы: {}", serversAddresses);
        var node = Node.getInstance();
        List<Callable<Pair<String, ResponseEntity<String>>>> tasks = getTasks(
                serversAddresses,
                address -> (ResponseEntity<String>) safeApiCall(restClient.post()
                        .uri(address + "/api/v1/set-leader")
                        .contentType(TEXT_PLAIN)
                        .body("http://host.docker.internal:%s".formatted(port) + " " + node.getCurrentTerm())
                        .retrieve()
                        .toBodilessEntity()));
        try {
            List<Future<Pair<String, ResponseEntity<String>>>> results = executor.invokeAll(tasks, 1, SECONDS);
            for (var r : results) {
                var result = r.get();
                if (result == null) {
                    log.warn("Запрос ноды завершился по таймауту");
                    continue;
                }
                var resultResponse = result.getRight();
                var address = result.getLeft();

                if (!resultResponse.getStatusCode().is2xxSuccessful()) {
                    log.warn("Нода с адресом {} получила ответ, отличающийся от 200", address);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Что-то пошло не так", e);
        }
    }

    private ResponseEntity<?> safeApiCall(ResponseEntity<?> response) {
        try {
            return response;
        } catch (Exception e) {
            log.warn("Что-то пошло не так", e);
            return null;
        }
    }

    @PreDestroy
    private void destroy() {
        executor.shutdown();
    }

}
