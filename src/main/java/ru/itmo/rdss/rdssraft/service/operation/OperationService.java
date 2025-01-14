package ru.itmo.rdss.rdssraft.service.operation;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.rdss.rdssraft.entity.ITable;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service("operationService")
public class OperationService implements IOperationService {

    private final ITable<String, String> storage;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public OperationService(ITable<String, String> storage) {
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
            storage.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String key) {
        lock.writeLock().lock();
        try {
            storage.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            storage.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void putAll(Map<String, String> map) {
        lock.writeLock().lock();
        try {
            for (var entry : map.entrySet()) {
                storage.put(entry.getKey(), entry.getValue());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

}
