package ru.itmo.rdss.rdssraft.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.rdss.rdssraft.config.StorageConfiguration;
import ru.itmo.rdss.rdssraft.exception.StorageNotFoundException;
import ru.itmo.rdss.rdssraft.util.MemoryUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Storage implements IStorage {

    private final Map<String, ITable<String, String>> storage = new HashMap<>();
    private final StorageConfiguration config;
    private final MemoryUtil memoryUtil;

    public Storage(StorageConfiguration config) {
        this.config = config;
        this.memoryUtil = new MemoryUtil(config.maxSizeByte());
    }

    @Override
    public void createTable(String tableName) {
        storage.put(tableName, new Table(memoryUtil));
    }

    @Override
    public void dropTable(String tableName) {
        storage.remove(tableName);
    }

    @Override
    public ITable<String, String> getTable(String tableName) {
        var table =  storage.get(tableName);
        if (table == null) {
            throw new StorageNotFoundException("Таблица с именем %s не найдена".formatted(tableName));
        }
        return table;
    }

    @Override
    public List<String> getAllTables() {
        return new ArrayList<>(storage.keySet());
    }

}
