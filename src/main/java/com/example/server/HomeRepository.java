package com.example.server;

import java.util.Map;
import java.util.UUID;

/**
 * Abstraction for home persistence.
 */
public interface HomeRepository {

    Map<UUID, Home> loadAll();

    void saveAll(Map<UUID, Home> homes);
}
