package ru.itmo.rdss.rdssraft.service.operation;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.itmo.rdss.rdssraft.entity.ITable;
import ru.itmo.rdss.rdssraft.util.TaskUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import static org.springframework.http.MediaType.TEXT_PLAIN;

@Slf4j
@Service("replicatedOperationService")
public class ReplicatedOperationService implements IOperationService {

    @Value("${storage.port}")
    private long port;

    private final ITable<String, String> storage;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final RestClient restClient = RestClient.create();

    public ReplicatedOperationService(ITable<String, String> storage) {
        this.storage = storage;
    }

    //Data manipulation
    @Override
    public Map<String, String> getAll() {
        return storage.getAll();
    }

    @Override
    public String get(String key) {
        return storage.get(key);
    }

    @Nullable
    @Override
    public String getIfPresent(String key) {
        return storage.getIfPresent(key);
    }

    @Override
    public void put(String key, String value) {
        lock.writeLock().lock();
        try {
            var previousValue = storage.getIfPresent(key);
            storage.put(key, value);
            var result = replicate(
                    address -> restClient.put()
                            .uri(address + "/api/v1/log/operations/key/" + key)
                            .contentType(TEXT_PLAIN)
                            .body(value)
                            .retrieve()
                            .body(String.class));
            if (!result) {
                log.info("Откат заполнение значения {} по ключу {}", key, value);
                if (previousValue == null) {
                    storage.remove(key);
                } else {
                    storage.put(key, previousValue);
                }
                throw new IllegalStateException("Произошла ошибка при репликации");
            }
            log.info("Инициализируется коммит в лог кластера");
            restClient.put()
                    .uri("http://host.docker.internal:8080/api/v1/log/operations/key/" + key)
                    .contentType(TEXT_PLAIN)
                    .body(value)
                    .retrieve()
                    .body(String.class);
            log.info("Операция закоммичена в лог");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String key) {
        lock.writeLock().lock();
        try {
            var previousValue = storage.get(key);
            storage.remove(key);

            var result = replicate(
                    address -> restClient.delete()
                            .uri(address + "/api/v1/log/operations/key/" + key)
                            .accept(TEXT_PLAIN)
                            .retrieve()
                            .body(String.class));
            if (!result) {
                storage.put(key, previousValue);
                throw new IllegalStateException("Произошла ошибка при репликации");
            }
            log.info("Инициализируется коммит в лог кластера");
            restClient.delete()
                    .uri("http://host.docker.internal:8080/api/v1/log/operations/key/" + key)
                    .accept(TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);
            log.info("Операция закоммичена в лог");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            var previousValue = new HashMap<>(storage.getAll());
            storage.clear();
            var result = replicate(
                    address -> restClient.delete()
                            .uri(address + "/api/v1/log/operations/clear")
                            .accept(TEXT_PLAIN)
                            .retrieve()
                            .body(String.class));
            if (!result) {
                for (var entry : previousValue.entrySet()) {
                    storage.put(entry.getKey(), entry.getValue());
                }
                throw new IllegalStateException("Произошла ошибка при репликации");
            }
            log.info("Инициализируется коммит в лог кластера");
            restClient.delete()
                    .uri("http://host.docker.internal:8080/api/v1/log/operations/clear")
                    .accept(TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);
            log.info("Операция закоммичена в лог");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean replicate(Function<String, String> request) {
        var serverAddresses = TaskUtil.getServersAddresses();
        var sentRequests = 0;
        for (var serverAddress : serverAddresses) {
            if (serverAddress.equals("http://host.docker.internal:" + port)) {
                sentRequests++;
                continue;
            }
            try {
                var response = request.apply(serverAddress);
                if (response.equals("ok")) {
                    sentRequests++;
                }
            } catch (Exception e) {
                log.warn("Произошла ошибка при попытке репликации на сервер {}", serverAddress, e);
            }
        }
        if (sentRequests > serverAddresses.size() / 2) {
            log.info("Репликация прошла успешно");
            return true;
        }
        return false;
    }

}
