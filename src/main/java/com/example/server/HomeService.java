package com.example.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Service layer for managing player homes with async persistence.
 */
public class HomeService {

    private final JavaPlugin plugin;
    private final HomeRepository repository;
    private final Map<UUID, Home> homeCache = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();

    private BukkitTask saveTask;
    private boolean dirty;

    public HomeService(JavaPlugin plugin, HomeRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void load() {
        homeCache.clear();
        homeCache.putAll(repository.loadAll());
    }

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
        scheduleSave();
    }

    public boolean hasHome(Player player) {
        return player != null && homeCache.containsKey(player.getUniqueId());
    }

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

    public void deleteHome(Player player) {
        if (player == null) {
            return;
        }

        homeCache.remove(player.getUniqueId());
        scheduleSave();
    }

    public void flush() {
        synchronized (saveLock) {
            if (saveTask != null) {
                saveTask.cancel();
                saveTask = null;
            }
        }
        saveSnapshotSync();
    }

    private void scheduleSave() {
        synchronized (saveLock) {
            if (saveTask != null) {
                dirty = true;
                return;
            }
            scheduleSaveLocked();
        }
    }

    private void scheduleSaveLocked() {
        dirty = false;
        saveTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            saveSnapshotAsync();
            synchronized (saveLock) {
                saveTask = null;
                if (dirty) {
                    scheduleSaveLocked();
                }
            }
        }, 40L);
    }

    private void saveSnapshotAsync() {
        Map<UUID, Home> snapshot = new HashMap<>(homeCache);
        repository.saveAll(snapshot);
    }

    private void saveSnapshotSync() {
        Map<UUID, Home> snapshot = new HashMap<>(homeCache);
        repository.saveAll(snapshot);
    }
}
