package ru.itmo.rdss.rdssraft.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.rdss.rdssraft.service.IOperationService;

import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/operations")
public class OperationController {

    private final IOperationService operationService;

    @GetMapping("/tables")
    public String getAllTables() {
        var tables = operationService.getAllTables();
        if (CollectionUtils.isEmpty(tables)) {
            return "[]";
        }
        return "[" + StringUtils.join(tables, ", ") + "]";
    }

    @PostMapping("/table/{table}")
    public void createTable(@PathVariable("table") String table) {
        operationService.createTable(table);
    }

    @DeleteMapping("/table/{table}")
    public void dropTable(@PathVariable("table") String table) {
        operationService.dropTable(table);
    }

    @GetMapping("/table/{table}")
    public String getAllValuesByTable(@PathVariable("table") String table) {
        var map = operationService.getAll(table);
        if (CollectionUtils.isEmpty(map)) {
            return "{}";
        }
        return map.entrySet()
                .stream()
                .map(it -> "\t" + it.getKey() + ": " + it.getValue())
                .collect(Collectors.joining("\n", "{\n", "\n}"));
    }

    @GetMapping("/table/{table}/key/{key}")
    public String getValueByTableAndKey(
            @PathVariable("table") String table,
            @PathVariable("key") String key,
            @RequestParam("required") Boolean required) {

        return required
                ? operationService.get(table, key)
                : operationService.getIfPresent(table, key);
    }

    @PostMapping("/table/{table}/key/{key}")
    public void addValueByTableAndKey(
            @PathVariable("table") String table,
            @PathVariable("key") String key,
            @RequestBody String value) {

        operationService.add(table, key, value);
    }

    @PutMapping("/table/{table}/key/{key}")
    public void putValueByTableAndKey(
            @PathVariable("table") String table,
            @PathVariable("key") String key,
            @RequestBody String value) {

        operationService.put(table, key, value);
    }

    @PatchMapping("/table/{table}/key/{key}")
    public void updateValueByTableAndKey(
            @PathVariable("table") String table,
            @PathVariable("key") String key,
            @RequestBody String value) {

        operationService.update(table, key, value);
    }

    @DeleteMapping("/table/{table}/key/{key}")
    public void deleteValueByTableAndKey(
            @PathVariable("table") String table,
            @PathVariable("key") String key) {

        operationService.remove(table, key);
    }

    @DeleteMapping("/table/{table}/clear")
    public void clearAllByTable(@PathVariable("table") String table) {
        operationService.clear(table);
    }

}
