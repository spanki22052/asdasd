package com.example.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Optimized service layer for managing player homes with async persistence.
 * Uses dedicated thread pool for I/O operations and SQLite for performance.
 */
public class HomeService {

    private final JavaPlugin plugin;
    private final HomeRepository repository;
    private final Map<UUID, Home> homeCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    private volatile boolean isShuttingDown = false;

    public HomeService(JavaPlugin plugin, HomeRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        // Dedicated thread pool for async I/O operations
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "HomeService-IO");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Load all homes asynchronously on startup
     */
    public void load() {
        executorService.submit(() -> {
            try {
                Map<UUID, Home> loaded = repository.loadAll();
                homeCache.clear();
                homeCache.putAll(loaded);
                plugin.getLogger().info("Loaded " + loaded.size() + " homes");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load homes: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Set player's home location
     */
    public void setHome(Player player) {
        if (player == null || player.getLocation() == null || player.getLocation().getWorld() == null) {
            return;
        }

        Location loc = player.getLocation();
        Home home = new Home(
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );

        homeCache.put(player.getUniqueId(), home);

        // Save asynchronously if using SQLite repository
        if (repository instanceof SqliteHomeRepository sqliteRepo) {
            executorService.submit(() -> {
                try {
                    sqliteRepo.saveHome(player.getUniqueId(), home);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save home for " + player.getName() + ": " + e.getMessage());
                }
            });
        }
    }

    /**
     * Check if player has a home
     */
    public boolean hasHome(Player player) {
        return player != null && homeCache.containsKey(player.getUniqueId());
    }

    /**
     * Get player's home location
     */
    public Location getHome(Player player) {
        if (player == null) {
            return null;
        }

        Home home = homeCache.get(player.getUniqueId());
        if (home == null) {
            return null;
        }

        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            return null;
        }

        return new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
    }

    /**
     * Delete player's home
     */
    public void deleteHome(Player player) {
        if (player == null) {
            return;
        }

        homeCache.remove(player.getUniqueId());

        // Delete asynchronously if using SQLite repository
        if (repository instanceof SqliteHomeRepository sqliteRepo) {
            executorService.submit(() -> {
                try {
                    sqliteRepo.deleteHome(player.getUniqueId());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to delete home for " + player.getName() + ": " + e.getMessage());
                }
            });
        }
    }

    /**
     * Flush and shutdown service gracefully
     */
    public void shutdown() {
        isShuttingDown = true;

        plugin.getLogger().info("Shutting down HomeService...");

        // Save all homes synchronously on shutdown
        try {
            Map<UUID, Home> snapshot = new HashMap<>(homeCache);
            repository.saveAll(snapshot);
            plugin.getLogger().info("Saved " + snapshot.size() + " homes");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save homes on shutdown: " + e.getMessage());
        }

        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close database connection if using SQLite
        if (repository instanceof SqliteHomeRepository sqliteRepo) {
            sqliteRepo.close();
        }

        plugin.getLogger().info("HomeService shutdown complete");
    }

    /**
     * Get cache statistics
     */
    public int getCachedHomesCount() {
        return homeCache.size();
    }
}
