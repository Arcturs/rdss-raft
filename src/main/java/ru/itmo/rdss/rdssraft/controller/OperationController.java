package ru.itmo.rdss.rdssraft.controller;

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
import ru.itmo.rdss.rdssraft.entity.Node;
import ru.itmo.rdss.rdssraft.service.operation.IOperationService;

import java.util.stream.Collectors;

import static ru.itmo.rdss.rdssraft.dictionary.NodeState.LEADER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/operations")
public class OperationController {

    private final IOperationService replicatedOperationService;

    @GetMapping("")
    public String getAllValues() {
        var map = replicatedOperationService.getAll();
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
                ? replicatedOperationService.get(key)
                : replicatedOperationService.getIfPresent(key);
    }

    @PutMapping("/key/{key}")
    public String putValueKey(@PathVariable("key") String key, @RequestBody String value) {
        checkNodeIsLeader();
        replicatedOperationService.put(key, value);
        return "ok";
    }

    @DeleteMapping("/key/{key}")
    public String deleteValueKey(@PathVariable("key") String key) {
        checkNodeIsLeader();
        replicatedOperationService.remove(key);
        return "ok";
    }

    @DeleteMapping("/clear")
    public String clearAll() {
        checkNodeIsLeader();
        replicatedOperationService.clear();
        return "ok";
    }

    private static void checkNodeIsLeader() {
        if (Node.getInstance().getState() != LEADER) {
            throw new IllegalCallerException("Данный узел не доступен на обновление данных");
        }
    }

}
