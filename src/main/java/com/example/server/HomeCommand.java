package com.example.server;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Команда для телепортации на сохранённый дом
 */
public class HomeCommand implements CommandExecutor {

    private HomeManager homeManager;

    public HomeCommand(HomeManager homeManager) {
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
            player.sendMessage("§cУ вас нет сохранённого дома! Используйте /sethome чтобы его установить.");
            return false;
        }

        Location home = homeManager.getHome(player);
        if (home == null) {
            player.sendMessage("§cОшибка при загрузке дома! Мир может быть удалён.");
            return false;
        }

        player.teleport(home);
        player.sendMessage("§aВы телепортировались домой!");
        return true;
    }
}
