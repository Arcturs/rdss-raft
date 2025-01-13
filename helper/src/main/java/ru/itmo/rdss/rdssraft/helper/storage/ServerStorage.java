package ru.itmo.rdss.rdssraft.helper.storage;

import lombok.experimental.UtilityClass;
import ru.itmo.rdss.rdssraft.helper.entity.Server;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ServerStorage {

    public static List<Server> SERVERS = new ArrayList<>();

    public static String MASTER_ADDRESS = null;

}
