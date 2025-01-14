package ru.itmo.rdss.rdssraft.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@Component
@RequiredArgsConstructor
public class MemoryUtil {

    @Value("${storage.maxSizeByte}")
    private long maxMemoryUsage;
    private final AtomicLong currentMemoryUsage = new AtomicLong(0);

    /**
     * <p>
     * Потребление памяти для объекта String в 64-битной JVM:
     * <ul>
     *   <li>16 байт - заголовок объекта</li>
     *   <li>4 байта - ссылка на массив байтов</li>
     *   <li>4 байта - хэш-код </li>
     *   <li>4 байта - длина строки</li>
     *   <li>2 байта на символ (так как UTF16)</li>
     * </ul>
     * <p>
     * Также объекты выравниваются до 8 байт.
     * </p>
     */
    public long estimateStringSize(String str) {
        if (str == null) {
            return 0;
        }
        long sizeWithoutLeveling = 16 + 4 + 4 + 4 + (str.length() * 2L);
        long leveling = 8 - sizeWithoutLeveling % 8;
        return sizeWithoutLeveling + (leveling == 8 ? 0 : leveling);
    }

    public long estimateSizeInBytes(String key, String value) {
        long size = 0;

        if (key != null) {
            size += estimateStringSize(key);
        }

        if (value != null) {
            size += estimateStringSize(value);
        }

        return size;
    }

}
