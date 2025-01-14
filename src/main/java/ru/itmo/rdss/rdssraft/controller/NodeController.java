package ru.itmo.rdss.rdssraft.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.rdss.rdssraft.dictionary.NodeState;
import ru.itmo.rdss.rdssraft.entity.Node;

import java.util.UUID;

@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class NodeController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/{candidateId}/vote")
    public ResponseEntity<String> requestVote(@PathVariable UUID candidateId) {
        var node = Node.getInstance();
        if (node.getHasVotedFor() == null) {
            node.setHasVotedFor(candidateId);
            return ResponseEntity.ok(String.valueOf(node.getCurrentTerm()));
        }
        return ResponseEntity.badRequest().body(String.valueOf(node.getCurrentTerm()));
    }

    @PostMapping("/set-leader")
    public ResponseEntity<String> leaderNotification(@RequestBody String leader) {
        var node = Node.getInstance();
        var params = leader.split(" ");
        var leaderTerm = Integer.parseInt(params[1]);
        if (leaderTerm >= node.getCurrentTerm()) {
            node.setHasVotedFor(null);
            node.setMasterAddress(params[0]);
            node.setCurrentTerm(leaderTerm);
            node.setMasterLastUpdated(System.currentTimeMillis());
            node.setState(NodeState.FOLLOWER);
            log.info("Нотификация от лидера {} была прочитана", leader);
            return ResponseEntity.ok("ok");
        }
        log.info("Лидер отстает, игнорирование нотификации");
        return ResponseEntity.ok("ignore-" + node.getCurrentTerm());
    }



}
