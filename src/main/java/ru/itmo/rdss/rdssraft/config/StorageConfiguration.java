package ru.itmo.rdss.rdssraft.config;

import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "storage")
public record StorageConfiguration(Long maxSizeByte, @Nullable String filePath){

    @ConstructorBinding
    public StorageConfiguration {

    }

}
