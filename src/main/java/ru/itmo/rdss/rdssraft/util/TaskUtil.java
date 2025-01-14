package ru.itmo.rdss.rdssraft.util;

import lombok.experimental.UtilityClass;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.MediaType.TEXT_PLAIN;

@UtilityClass
public class TaskUtil {

    private static final RestClient REST_CLIENT = RestClient.create();

    public static List<String> getServersAddresses() {
        var response = REST_CLIENT.get()
                .uri("http://host.docker.internal:8080/api/v1/cluster/servers")
                .accept(TEXT_PLAIN)
                .retrieve()
                .body(String.class);
        return Arrays.stream(response.replace("[", "")
                        .replace("]", "")
                        .split(", "))
                .toList();
    }

}
