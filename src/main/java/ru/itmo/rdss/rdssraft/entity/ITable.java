package ru.itmo.rdss.rdssraft.entity;

import jakarta.annotation.Nullable;

import java.util.Map;

//String String
public interface ITable<K, V> {

    Map<String, String> getAll();

    V get(K key);

    @Nullable
    V getIfPresent(K key);

    void put(K key, V value);

    void remove(K key);

    void clear();

}
