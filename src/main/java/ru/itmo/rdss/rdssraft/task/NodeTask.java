package ru.itmo.rdss.rdssraft.task;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.itmo.rdss.rdssraft.entity.Node;
import ru.itmo.rdss.rdssraft.service.operation.OperationService;
import ru.itmo.rdss.rdssraft.util.TaskUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
@ConditionalOnProperty(value = "node.type", havingValue = "node")
public class NodeTask {

    private final RestClient restClient = RestClient.create();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private final String port;
    private final OperationService operationService;

    public NodeTask(OperationService operationService, @Value("${storage.port}") String port) {
        this.operationService = operationService;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void execute() {
        Executors.newSingleThreadExecutor()
                .execute(this::run);
    }

    private void run() {
        var node = Node.getInstance();
        log.info("Запуск ноды с ИД {}", node.getId());
        restClient.post()
                .uri("http://host.docker.internal:8080/api/v1/cluster/server")
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
                    .uri("http://host.docker.internal:8080/api/v1/cluster/leader")
                    .accept(TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);
            if (StringUtils.isBlank(leaderAddress)) {
                log.info("Мастера нет в кластере, нода становится кандидатом");
                node.setState(CANDIDATE);
                return;
            }
            node.setMasterAddress(leaderAddress);
            log.info("Получен мастер: {}. Реплицируем лог кластера", leaderAddress);
            replicateLog();
            log.info("Лог реплицирован");
            node.setMasterLastUpdated(System.currentTimeMillis());
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

    public void replicateLog() {
        var response = restClient.get()
                .uri("http://host.docker.internal:8080/api/v1/log/operations")
                .retrieve()
                .body(String.class);
        if (response.equals("{}")) {
            return;
        }
        var clearedResponse = response.replace("{\n", "")
                .replace("\n}", "")
                .replace("\t", "");
        var map = new HashMap<String, String>();
        Arrays.stream(clearedResponse.split("\n"))
                .map(it -> it.split(": "))
                .forEach(it -> map.put(it[0], it[1]));
        operationService.clear();
        operationService.putAll(map);
    }

    private void election() {
        log.info("Начало голосования!");
        var serversAddresses = TaskUtil.getServersAddresses();
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
                    .uri("http://host.docker.internal:8080/api/v1/cluster/leader")
                    .contentType(TEXT_PLAIN)
                    .body("http://host.docker.internal:" + port)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Нода становится лидером!");

            log.info("Репликация из кластера");
            replicateLog();
        } else {
            node.setState(FOLLOWER);
            log.info("Голоса не собраны, голосование не состоялось");
        }
        node.setHasVotedFor(null);
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
        var serversAddresses = TaskUtil.getServersAddresses();
        log.info("Узлы: {}", serversAddresses);
        var node = Node.getInstance();
        List<Callable<Pair<String, ResponseEntity<String>>>> tasks = getTasks(
                serversAddresses,
                address -> (ResponseEntity<String>) safeApiCall(restClient.post()
                        .uri(address + "/api/v1/set-leader")
                        .contentType(TEXT_PLAIN)
                        .body("http://host.docker.internal:%s".formatted(port) + " " + node.getCurrentTerm())
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
                var responseBody = resultResponse.getBody();
                if (responseBody.contains("ignore")) {
                    log.info("Нода отстает, она переходит в состояние follower");
                    node.setState(FOLLOWER);
                    node.setHasVotedFor(null);
                    node.setCurrentTerm(Integer.parseInt(responseBody.substring(7)));
                    node.setMasterAddress(null);
                    return;
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
        try {
            if (!executor.awaitTermination(30, SECONDS)) {
                throw new InterruptedException();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

}
