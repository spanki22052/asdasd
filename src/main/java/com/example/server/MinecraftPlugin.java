package com.example.server;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class with optimized initialization
 */
public class MinecraftPlugin extends JavaPlugin {

    private HomeService homeService;
    private HomeRepository repository;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        getLogger().info("Initializing MyServer plugin...");

        // Save default config
        saveDefaultConfig();

        // Initialize repository (SQLite by default, YAML as fallback)
        String storageType = getConfig().getString("storage-type", "sqlite");

        if ("sqlite".equalsIgnoreCase(storageType)) {
            getLogger().info("Using SQLite storage for optimal performance");
            repository = new SqliteHomeRepository(this);
        } else {
            getLogger().info("Using YAML storage (consider switching to SQLite for better performance)");
            repository = new YamlHomeRepository(this);
        }

        // Initialize home service with async loading
        homeService = new HomeService(this, repository);
        homeService.load();

        // Register commands efficiently
        registerCommand("hello", new HelloCommand());
        registerCommand("sethome", new SetHomeCommand(homeService));
        registerCommand("home", new HomeCommand(homeService));
        registerCommand("delhome", new DeleteHomeCommand(homeService));

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("MyServer plugin enabled in " + loadTime + "ms");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling MyServer plugin...");

        if (homeService != null) {
            homeService.shutdown();
        }

        getLogger().info("MyServer plugin disabled");
    }

    /**
     * Register command with null check
     */
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml");
        }
    }

    /**
     * Get home service instance (for API usage)
     */
    public HomeService getHomeService() {
        return homeService;
    }
}
