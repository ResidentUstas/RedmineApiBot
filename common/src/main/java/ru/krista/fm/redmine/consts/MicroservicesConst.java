package ru.krista.fm.redmine.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

public final class MicroservicesConst {

    public static final String ASSOCIATIONMANAGER = "associationmanager";
    public static final String ENTITYMANAGER = "entitymanager";
    public static final String TABLEMANAGER = "tablemanager";

    public static final Microservice ASSOCIATIONMANAGER_SERVICE = new Microservice(UUID.fromString("e37031e1-ddf5-4d7a-9650-c29f53750ca7"), ASSOCIATIONMANAGER);
    public static final Microservice ENTITYMANAGER_SERVICE = new Microservice(UUID.fromString("ec8d4c71-ddeb-4407-9a4c-70c6851fe251"), ENTITYMANAGER);
    public static final Microservice TABLEMANAGER_SERVICE = new Microservice(UUID.fromString("66350375-4e6d-4f1b-9dc3-bf6e3161ac5b"), TABLEMANAGER);

    public static Microservice getToFromTarget(String name) throws Exception {
        return allService.stream().filter(x->x.name.equals(name)).findFirst().orElseThrow(() -> new Exception("unknown target"));
    }

    public static Microservice getByUuid(UUID uuid) {
        return allService.stream().filter(x->x.uuid.equals(uuid)).findFirst().orElse(null);
    }

    private static final Set<Microservice> allService = Set.of(ASSOCIATIONMANAGER_SERVICE, ENTITYMANAGER_SERVICE, TABLEMANAGER_SERVICE);

    private MicroservicesConst() { }

    @Getter
    @AllArgsConstructor
    public static final class Microservice {
        private final UUID uuid;
        private final String name;
    }
}
