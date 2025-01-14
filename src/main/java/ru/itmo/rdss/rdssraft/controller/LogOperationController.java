package ru.itmo.rdss.rdssraft.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.rdss.rdssraft.service.operation.IOperationService;

import java.util.stream.Collectors;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/log/operations")
public class LogOperationController {

    private final IOperationService operationService;

    @GetMapping("")
    public String getAllValues() {
        var map = operationService.getAll();
        if (CollectionUtils.isEmpty(map)) {
            return "{}";
        }
        return map.entrySet()
                .stream()
                .map(it -> "\t" + it.getKey() + ": " + it.getValue())
                .collect(Collectors.joining("\n", "{\n", "\n}"));
    }

    @GetMapping("/key/{key}")
    public String getValueByKey(@PathVariable("key") String key, @RequestParam("required") Boolean required) {
        return required
                ? operationService.get(key)
                : operationService.getIfPresent(key);
    }

    @PutMapping("/key/{key}")
    public String putValueKey(@PathVariable("key") String key, @RequestBody String value) {
        operationService.put(key, value);
        return "ok";
    }

    @DeleteMapping("/key/{key}")
    public String deleteValueKey(@PathVariable("key") String key) {
        operationService.remove(key);
        return "ok";
    }

    @DeleteMapping("/clear")
    public String clearAll() {
        operationService.clear();
        return "ok";
    }

}
