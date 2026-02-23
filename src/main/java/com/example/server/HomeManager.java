package com.example.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private Map<String, Location> homeCache = new HashMap<>();

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
     * Загружает конфигурацию домов и заполняет кэш
     */
    private synchronized void loadHomes() {
        this.homesConfig = YamlConfiguration.loadConfiguration(homesFile);

        // Заполняем кэш из конфигурации
        homeCache.clear();
        if (homesConfig.contains("homes")) {
            for (String playerName : homesConfig.getConfigurationSection("homes").getKeys(false)) {
                String path = "homes." + playerName;
                String worldName = homesConfig.getString(path + ".world");

                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    double x = homesConfig.getDouble(path + ".x");
                    double y = homesConfig.getDouble(path + ".y");
                    double z = homesConfig.getDouble(path + ".z");
                    float yaw = (float) homesConfig.getDouble(path + ".yaw");
                    float pitch = (float) homesConfig.getDouble(path + ".pitch");

                    Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                    homeCache.put(playerName, loc);
                }
            }
        }
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
     * Сохраняет дом для игрока с проверкой ошибок
     */
    public void setHome(Player player) {
        if (!isInitialized) {
            plugin.getLogger().warning("HomeManager не инициализирован!");
            return;
        }

        if (player == null) {
            plugin.getLogger().warning("Попытка сохранить дом для null Player!");
            return;
        }

        if (player.getLocation() == null) {
            plugin.getLogger().warning("Player локация = null для " + player.getName());
            return;
        }

        String playerName = player.getName();
        Location loc = player.getLocation();

        if (loc.getWorld() == null) {
            plugin.getLogger().warning("Мир игрока " + playerName + " = null!");
            return;
        }

        // Сохраняем координаты в конфиг
        String path = "homes." + playerName;
        homesConfig.set(path + ".world", loc.getWorld().getName());
        homesConfig.set(path + ".x", loc.getX());
        homesConfig.set(path + ".y", loc.getY());
        homesConfig.set(path + ".z", loc.getZ());
        homesConfig.set(path + ".yaw", loc.getYaw());
        homesConfig.set(path + ".pitch", loc.getPitch());

        // Обновляем кэш
        homeCache.put(playerName, loc.clone());

        saveHomes();
    }

    /**
     * Получает дом игрока из кэша для быстрого доступа
     */
    public Location getHome(Player player) {
        if (!isInitialized || player == null) {
            return null;
        }

        String playerName = player.getName();

        // Сначала проверяем кэш
        if (homeCache.containsKey(playerName)) {
            Location cachedHome = homeCache.get(playerName);
            // Проверяем, что мир ещё существует
            if (cachedHome.getWorld() != null) {
                return cachedHome.clone();
            } else {
                // Мир был удалён, удаляем из кэша
                homeCache.remove(playerName);
                homesConfig.set("homes." + playerName, null);
                saveHomes();
                return null;
            }
        }

        // Если в кэше нет, загружаем с диска
        String path = "homes." + playerName;
        if (!homesConfig.contains(path)) {
            return null;
        }

        String worldName = homesConfig.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        // Проверяем, существует ли мир
        if (Bukkit.getWorld(worldName) == null) {
            plugin.getLogger().warning("Мир " + worldName + " для игрока " + playerName + " не найден!");
            return null;
        }

        double x = homesConfig.getDouble(path + ".x");
        double y = homesConfig.getDouble(path + ".y");
        double z = homesConfig.getDouble(path + ".z");
        float yaw = (float) homesConfig.getDouble(path + ".yaw");
        float pitch = (float) homesConfig.getDouble(path + ".pitch");

        Location home = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);

        // Кэшируем загруженную локацию
        homeCache.put(playerName, home.clone());

        return home;
    }

    /**
     * Проверяет, есть ли дом у игрока
     */
    public boolean hasHome(Player player) {
        if (!isInitialized || player == null) {
            return false;
        }
        String playerName = player.getName();
        // Сначала проверяем кэш для быстрого доступа
        if (homeCache.containsKey(playerName)) {
            return true;
        }
        return homesConfig.contains("homes." + playerName);
    }

    /**
     * Удаляет дом игрока
     */
    public void deleteHome(Player player) {
        if (!isInitialized || player == null) {
            return;
        }

        String playerName = player.getName();

        // Удаляем из кэша
        homeCache.remove(playerName);

        // Удаляем из конфига
        homesConfig.set("homes." + playerName, null);

        saveHomes();
    }
}
