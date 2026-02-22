package com.example.server;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда для сохранения текущей локации как дома
 */
public class SetHomeCommand implements CommandExecutor {

    private HomeManager homeManager;

    public SetHomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return false;
        }

        Player player = (Player) sender;
        homeManager.setHome(player);
        player.sendMessage("§aДом успешно сохранён!");
        return true;
    }
}
