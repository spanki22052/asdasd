package com.example.server;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Improved Home command with teleport delay and cooldown
 */
public class ImprovedHomeCommand implements CommandExecutor {

    private final HomeService homeService;
    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Location> teleportQueue = new HashMap<>();

    public ImprovedHomeCommand(HomeService homeService, JavaPlugin plugin) {
        this.homeService = homeService;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return false;
        }

        if (!homeService.hasHome(player)) {
            player.sendMessage("§cУ вас нет сохранённого дома! Используйте /sethome");
            return false;
        }

        // Check cooldown
        int cooldownSeconds = plugin.getConfig().getInt("homes.teleport-cooldown", 3);
        if (cooldownSeconds > 0) {
            long lastTeleport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            long timeSince = (System.currentTimeMillis() - lastTeleport) / 1000;

            if (timeSince < cooldownSeconds) {
                long remaining = cooldownSeconds - timeSince;
                player.sendMessage("§cПодождите " + remaining + " сек. перед следующей телепортацией");
                return true;
            }
        }

        Location home = homeService.getHome(player);
        if (home == null) {
            player.sendMessage("§cОшибка при загрузке дома! Мир может быть удалён.");
            return false;
        }

        // Teleport delay
        int delaySeconds = plugin.getConfig().getInt("homes.teleport-delay", 3);
        if (delaySeconds > 0) {
            Location startLocation = player.getLocation().clone();
            teleportQueue.put(player.getUniqueId(), startLocation);

            player.sendMessage("§aТелепортация через " + delaySeconds + " сек. Не двигайтесь!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!teleportQueue.containsKey(player.getUniqueId())) {
                    return; // Already cancelled
                }

                Location currentLocation = player.getLocation();
                if (currentLocation.distanceSquared(startLocation) > 0.1) {
                    player.sendMessage("§cТелепортация отменена! Вы сдвинулись с места.");
                    teleportQueue.remove(player.getUniqueId());
                    return;
                }

                executeTeleport(player, home);
                teleportQueue.remove(player.getUniqueId());
            }, delaySeconds * 20L);
        } else {
            executeTeleport(player, home);
        }

        return true;
    }

    private void executeTeleport(Player player, Location home) {
        player.teleport(home);
        player.sendMessage("§aВы телепортировались домой!");
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
