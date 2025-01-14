package ru.itmo.rdss.rdssraft.service.operation;

import jakarta.annotation.Nullable;

import java.util.Map;

public interface IOperationService {

    Map<String, String> getAll();

    String get(String key);

    @Nullable
    String getIfPresent(String key);

    void put(String key, String value);

    void remove(String key);

    void clear();

}
