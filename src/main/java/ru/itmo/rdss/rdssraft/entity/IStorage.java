package ru.itmo.rdss.rdssraft.entity;

import java.util.List;

public interface IStorage {

    void createTable(String tableName);

    void dropTable(String tableName);

    ITable<String, String> getTable(String tableName);

    List<String> getAllTables();


}
