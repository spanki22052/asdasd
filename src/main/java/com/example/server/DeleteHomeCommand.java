package com.example.server;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда для удаления сохранённого дома
 */
public class DeleteHomeCommand implements CommandExecutor {

    private HomeManager homeManager;

    public DeleteHomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (homeManager == null) {
            sender.sendMessage("§cОшибка плагина! HomeManager не инициализирован.");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return false;
        }

        Player player = (Player) sender;

        if (!homeManager.hasHome(player)) {
            player.sendMessage("§cУ вас нет сохранённого дома!");
            return false;
        }

        homeManager.deleteHome(player);
        player.sendMessage("§aДом успешно удалён!");
        return true;
    }
}
