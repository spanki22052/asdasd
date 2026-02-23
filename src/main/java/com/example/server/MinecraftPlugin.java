package com.example.server;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Minecraft server
 */
public class MinecraftPlugin extends JavaPlugin {

    private HomeManager homeManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("MyServer plugin has been enabled!");

        // Инициализируем manager для домов
        homeManager = new HomeManager(this);

        if (homeManager == null) {
            getLogger().severe("Failed to initialize HomeManager!");
            return;
        }

        // Register commands and event listeners here
        if (getCommand("hello") != null) {
            getCommand("hello").setExecutor(new HelloCommand());
        }
        if (getCommand("sethome") != null) {
            getCommand("sethome").setExecutor(new SetHomeCommand(homeManager));
        }
        if (getCommand("home") != null) {
            getCommand("home").setExecutor(new HomeCommand(homeManager));
        }
        if (getCommand("delhome") != null) {
            getCommand("delhome").setExecutor(new DeleteHomeCommand(homeManager));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("MyServer plugin has been disabled!");
    }
}
