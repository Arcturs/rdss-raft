package ru.itmo.rdss.rdssraft.entity;

import ru.itmo.rdss.rdssraft.config.StorageConfiguration;

import java.util.List;
import java.util.Map;

public interface IStorage {

    void createTable(String tableName);

    void dropTable(String tableName);

    ITable<String, String> getTable(String tableName);

    Map<String, Map<String, String>> getStorage();

    List<String> getAllTables();

    StorageConfiguration getConfig();


}
