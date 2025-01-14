package ru.itmo.rdss.rdssraft.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.rdss.rdssraft.service.ClusterService;

@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cluster")
public class ClusterController {

    private final ClusterService clusterService;

    @GetMapping("/servers")
    public String getServers() {
        return clusterService.getServers();
    }

    @GetMapping("/leader")
    public String getLeader() {
        return clusterService.getLeader();
    }

    @PostMapping("/leader")
    public void setLeader(@RequestBody String leader) {
        clusterService.setLeader(leader);
    }

    @PostMapping("/server")
    public void writeServer(@RequestBody String server) {
        clusterService.addServer(server);
    }

}
