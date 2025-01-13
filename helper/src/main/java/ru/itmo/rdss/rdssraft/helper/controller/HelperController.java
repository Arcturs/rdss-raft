package ru.itmo.rdss.rdssraft.helper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.rdss.rdssraft.helper.service.HelperService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HelperController {

    private final HelperService helperService;

    @GetMapping("/servers")
    public String getServers() {
        return helperService.getServers();
    }

    @GetMapping("/leader")
    public String getLeader() {
        return helperService.getLeader();
    }

    @PostMapping("/leader")
    public void setLeader(@RequestBody String leader) {
        helperService.setLeader(leader);
    }

    @PostMapping("/server")
    public void writeServer(@RequestBody String server) {
        helperService.addServer(server);
    }

}
