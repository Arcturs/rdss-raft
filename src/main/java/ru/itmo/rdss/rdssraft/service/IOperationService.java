package ru.itmo.rdss.rdssraft.service;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

public interface IOperationService {

    void createTable(String tableName);

    void dropTable(String tableName);

    List<String> getAllTables();

    Map<String, String> getAll(String tableName);

    String get(String tableName, String key);

    @Nullable
    String getIfPresent(String tableName, String key);

    void add(String tableName, String key, String value);

    void put(String tableName, String key, String value);

    void update(String tableName, String key, String value);

    void remove(String tableName, String key);

    void clear(String tableName);

}
