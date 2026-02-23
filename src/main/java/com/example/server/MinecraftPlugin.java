package com.example.server;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Minecraft server
 */
public class MinecraftPlugin extends JavaPlugin {

    private HomeService homeService;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("MyServer plugin has been enabled!");

        // Инициализируем сервис домов
        HomeRepository repository = new YamlHomeRepository(this);
        homeService = new HomeService(this, repository);
        homeService.load();

        // Register commands and event listeners here
        if (getCommand("hello") != null) {
            getCommand("hello").setExecutor(new HelloCommand());
        }
        if (getCommand("sethome") != null) {
            getCommand("sethome").setExecutor(new SetHomeCommand(homeService));
        }
        if (getCommand("home") != null) {
            getCommand("home").setExecutor(new HomeCommand(homeService));
        }
        if (getCommand("delhome") != null) {
            getCommand("delhome").setExecutor(new DeleteHomeCommand(homeService));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("MyServer plugin has been disabled!");
        if (homeService != null) {
            homeService.flush();
        }
    }
}
