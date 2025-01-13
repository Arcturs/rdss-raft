package ru.itmo.rdss.rdssraft.service;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.rdss.rdssraft.entity.IStorage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
public class OperationService implements IOperationService {

    private final IStorage storage;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public OperationService(IStorage storage) {
        this.storage = storage;
    }

    //Table manipulation
    @Override
    public void createTable(String tableName) {
        lock.writeLock().lock();
        try {
            log.info("Creating table {}", tableName);
            storage.createTable(tableName);
        } finally {
            lock.writeLock().unlock();
        }
        log.info("Created table {}", storage.getTable(tableName));
    }

    @Override
    public void dropTable(String tableName) {
        lock.writeLock().lock();
        try {
            log.info("Dropping table {}", tableName);
            storage.dropTable(tableName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<String> getAllTables() {
        return storage.getAllTables();
    }

    //Data manipulation
    @Override
    public Map<String, String> getAll(String tableName) {
        return storage.getTable(tableName).getAll();
    }

    @Override
    public String get(String tableName, String key) {
        return storage.getTable(tableName).get(key);
    }

    @Nullable
    @Override
    public String getIfPresent(String tableName, String key) {
        return storage.getTable(tableName).getIfPresent(key);
    }

    @Override
    public void add(String tableName, String key, String value) {
        lock.writeLock().lock();
        try {
            storage.getTable(tableName).add(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void put(String tableName, String key, String value) {
        lock.writeLock().lock();
        try {
            storage.getTable(tableName).put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(String tableName, String key, String value) {
        lock.writeLock().lock();
        try {
            storage.getTable(tableName).update(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String tableName, String key) {
        lock.writeLock().lock();
        try {
            storage.getTable(tableName).remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear(String tableName) {
        lock.writeLock().lock();
        try {
            storage.getTable(tableName).clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
