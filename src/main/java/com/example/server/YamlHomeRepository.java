package com.example.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YAML-backed repository for player homes.
 */
public class YamlHomeRepository implements HomeRepository {

    private final JavaPlugin plugin;
    private final File homesFile;

    public YamlHomeRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать файл homes.yml!");
            }
        }
    }

    @Override
    public Map<UUID, Home> loadAll() {
        Map<UUID, Home> result = new HashMap<>();
        FileConfiguration config = YamlConfiguration.loadConfiguration(homesFile);
        if (!config.contains("homes")) {
            return result;
        }

        ConfigurationSection homesSection = config.getConfigurationSection("homes");
        if (homesSection == null) {
            return result;
        }

        for (String uuidString : homesSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String path = "homes." + uuidString;
                String world = config.getString(path + ".world");
                if (world == null) {
                    continue;
                }

                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw");
                float pitch = (float) config.getDouble(path + ".pitch");

                result.put(uuid, new Home(world, x, y, z, yaw, pitch));
            } catch (IllegalArgumentException ex) {
                UUID migratedUuid = Bukkit.getOfflinePlayer(uuidString).getUniqueId();
                String path = "homes." + uuidString;
                String world = config.getString(path + ".world");
                if (world == null) {
                    continue;
                }

                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw");
                float pitch = (float) config.getDouble(path + ".pitch");

                result.put(migratedUuid, new Home(world, x, y, z, yaw, pitch));
                plugin.getLogger().info("Миграция дома: " + uuidString + " -> " + migratedUuid);
            }
        }

        return result;
    }

    @Override
    public void saveAll(Map<UUID, Home> homes) {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Home> entry : homes.entrySet()) {
            String path = "homes." + entry.getKey();
            Home home = entry.getValue();
            config.set(path + ".world", home.world());
            config.set(path + ".x", home.x());
            config.set(path + ".y", home.y());
            config.set(path + ".z", home.z());
            config.set(path + ".yaw", home.yaw());
            config.set(path + ".pitch", home.pitch());
        }

        try {
            config.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить homes.yml!");
        }
    }
}
