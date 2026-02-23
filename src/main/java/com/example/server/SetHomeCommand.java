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

    private final HomeService homeService;

    public SetHomeCommand(HomeService homeService) {
        this.homeService = homeService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (homeService == null) {
            sender.sendMessage("§cОшибка плагина! HomeService не инициализирован.");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return false;
        }

        Player player = (Player) sender;
        homeService.setHome(player);
        player.sendMessage("§aДом успешно сохранён!");
        player.sendMessage("§7Локация: §f" + player.getLocation().getBlockX() + ", "
                + player.getLocation().getBlockY() + ", "
                + player.getLocation().getBlockZ());
        return true;
    }
}
