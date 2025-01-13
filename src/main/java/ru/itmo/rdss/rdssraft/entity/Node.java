package ru.itmo.rdss.rdssraft.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.itmo.rdss.rdssraft.dictionary.NodeState;

import java.util.Random;
import java.util.UUID;

import static ru.itmo.rdss.rdssraft.dictionary.NodeState.FOLLOWER;

@Data
@Accessors(chain = true)
public final class Node {

    private final Long heartbeatTimeout = 100L;

    private static Node instance;

    private UUID id;
    private NodeState state;
    private Integer currentTerm;
    private String masterAddress;
    private Long masterLastUpdated;
    private UUID hasVotedFor;

    private Node() {
        this.id = UUID.randomUUID();
        this.state = FOLLOWER;
        this.currentTerm = 0;
        this.masterLastUpdated = 0L;
        this.hasVotedFor = null;
    }

    public static Node getInstance() {
        if (instance == null) {
            instance = new Node();
        }
        return instance;
    }

    public static long getElectionTimeout() {
        return (new Random().nextInt(150) + 300L);
    }

}
