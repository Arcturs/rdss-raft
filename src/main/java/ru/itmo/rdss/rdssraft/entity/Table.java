package ru.itmo.rdss.rdssraft.entity;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.rdss.rdssraft.exception.StorageNotFoundException;
import ru.itmo.rdss.rdssraft.exception.StorageOverflowException;
import ru.itmo.rdss.rdssraft.util.MemoryUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * Реализация предназначена только для x64.
 * </p>
 * <p>
 * Реализация String/String хранилища.
 * </p>
 */
@Slf4j
@Component
public class Table implements ITable<String, String> {

    private static final long GIGABYTE = 1024 * 1024 * 1024;

    private final Map<String, String> table;
    private final MemoryUtil memoryUtil;
    private final long maxMemoryUsage;
    private final AtomicLong currentTableMemoryUsage = new AtomicLong(0);

    public Table(MemoryUtil memoryUtil) {
        this.memoryUtil = memoryUtil;
        this.maxMemoryUsage = memoryUtil.getMaxMemoryUsage();
        this.table = new ConcurrentHashMap<>();
    }

    @Override
    public Map<String, String> getAll() {
        return table;
    }

    @Override
    public String get(String key) {
        var value = table.get(key);
        if (value == null) {
            throw new StorageNotFoundException("Объект с ключом '" + key + "' не найден");
        }
        return value;
    }

    @Nullable
    @Override
    public String getIfPresent(String key) {
        return table.get(key);
    }

    @Override
    public void put(String key, String value) {
        long entrySize = memoryUtil.estimateSizeInBytes(key, value);

        if (memoryUtil.getCurrentMemoryUsage().get() + entrySize > maxMemoryUsage) {
            throw new StorageOverflowException("Превышен лимит памяти в " + maxMemoryUsage / GIGABYTE + " ГБ");
        }

        table.put(key, value);
        memoryUtil.getCurrentMemoryUsage().addAndGet(entrySize);
        currentTableMemoryUsage.addAndGet(entrySize);
    }

    @Override
    public void remove(String key) {
        String oldValue = table.remove(key);

        if (oldValue != null) {
            long entrySize = memoryUtil.estimateSizeInBytes(key, oldValue);
            memoryUtil.getCurrentMemoryUsage().addAndGet(-entrySize);
            currentTableMemoryUsage.addAndGet(-entrySize);
        } else {
            throw new StorageNotFoundException("Объект с ключом '" + key + "' не найден");
        }
    }

    @Override
    public void clear() {
        table.clear();
        memoryUtil.getCurrentMemoryUsage().addAndGet(-currentTableMemoryUsage.get());
        currentTableMemoryUsage.set(0);
    }

}
