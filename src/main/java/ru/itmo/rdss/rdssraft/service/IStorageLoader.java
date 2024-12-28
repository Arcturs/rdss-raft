package ru.itmo.rdss.rdssraft.service;

import ru.itmo.rdss.rdssraft.config.StorageConfiguration;
import ru.itmo.rdss.rdssraft.entity.IStorage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public interface IStorageLoader {

    IStorage load(StorageConfiguration config);

    default CompletableFuture<IStorage> asyncLoad(Executor executor, StorageConfiguration config) {
        return CompletableFuture.supplyAsync(() -> load(config), executor)
                .exceptionally(ex -> {
                    throw new CompletionException(ex);
                });
    }

    void save(IStorage storage, StorageConfiguration config);

    default CompletableFuture<Void> asyncSave(IStorage storage, StorageConfiguration config, Executor executor) {
        return CompletableFuture.runAsync(() -> save(storage, config), executor)
                .exceptionally(ex -> {
                    throw new CompletionException(ex);
                });
    }
}
