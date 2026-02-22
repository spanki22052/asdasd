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

        // Register commands and event listeners here
        getCommand("hello").setExecutor(new HelloCommand());
        getCommand("sethome").setExecutor(new SetHomeCommand(homeManager));
        getCommand("home").setExecutor(new HomeCommand(homeManager));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("MyServer plugin has been disabled!");
    }
}
