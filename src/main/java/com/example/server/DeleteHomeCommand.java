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

    private final HomeService homeService;

    public DeleteHomeCommand(HomeService homeService) {
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

        if (!homeService.hasHome(player)) {
            player.sendMessage("§cУ вас нет сохранённого дома!");
            return false;
        }

        homeService.deleteHome(player);
        player.sendMessage("§aДом успешно удалён!");
        return true;
    }
}
