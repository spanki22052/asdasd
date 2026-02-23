package com.example.server;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manager для управления домами игроков
 */
public class HomeManager {

    private JavaPlugin plugin;
    private File homesFile;
    private FileConfiguration homesConfig;
    private boolean isInitialized = false;

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");

        // Создаём папку для плагина если её нет
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Создаём файл если его нет
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
                isInitialized = true;
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать файл homes.yml!");
                return;
            }
        }

        loadHomes();
        isInitialized = true;
    }

    /**
     * Загружает конфигурацию домов
     */
    private synchronized void loadHomes() {
        this.homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    /**
     * Сохраняет конфигурацию на диск (асинхронно, не блокирует главный поток)
     */
    private void saveHomes() {
        if (!isInitialized) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try {
                    homesConfig.save(homesFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Не удалось сохранить homes.yml!");
                }
            }
        });
    }

    /**
     * Сохраняет дом для игрока
     */
    public void setHome(Player player) {
        if (!isInitialized || player == null || player.getLocation() == null) {
            return;
        }
        String playerName = player.getName();
        Location loc = player.getLocation();

        // Сохраняем координаты в конфиг
        String path = "homes." + playerName;
        homesConfig.set(path + ".world", loc.getWorld().getName());
        homesConfig.set(path + ".x", loc.getX());
        homesConfig.set(path + ".y", loc.getY());
        homesConfig.set(path + ".z", loc.getZ());
        homesConfig.set(path + ".yaw", loc.getYaw());
        homesConfig.set(path + ".pitch", loc.getPitch());

        saveHomes();
    }

    /**
     * Получает дом игрока (берёт из памяти, не перезагружает с диска)
     */
    public Location getHome(Player player) {
        if (!isInitialized || player == null) {
            return null;
        }

        String playerName = player.getName();
        String path = "homes." + playerName;

        if (!homesConfig.contains(path)) {
            return null;
        }

        String worldName = homesConfig.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        double x = homesConfig.getDouble(path + ".x");
        double y = homesConfig.getDouble(path + ".y");
        double z = homesConfig.getDouble(path + ".z");
        float yaw = (float) homesConfig.getDouble(path + ".yaw");
        float pitch = (float) homesConfig.getDouble(path + ".pitch");

        // Проверяем, существует ли мир
        if (Bukkit.getWorld(worldName) == null) {
            return null;
        }

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    /**
     * Проверяет, есть ли дом у игрока
     */
    public boolean hasHome(Player player) {
        if (!isInitialized || player == null) {
            return false;
        }
        return homesConfig.contains("homes." + player.getName());
    }
}
