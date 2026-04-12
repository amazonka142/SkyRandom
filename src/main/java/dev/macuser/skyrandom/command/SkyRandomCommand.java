package dev.macuser.skyrandom.command;

import dev.macuser.skyrandom.game.Arena;
import dev.macuser.skyrandom.game.GameManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class SkyRandomCommand implements TabExecutor {

    private final GameManager gameManager;

    public SkyRandomCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "list" -> {
                Collection<Arena> arenas = gameManager.getArenas();
                if (arenas.isEmpty()) {
                    gameManager.sendLocalized(sender, "command.arenas_not_loaded");
                    return true;
                }

                gameManager.sendLocalized(sender, "command.available_arenas");
                for (Arena arena : arenas) {
                    gameManager.sendLocalized(
                        sender,
                        "command.arena_entry",
                        "display", arena.getDisplayName(),
                        "id", arena.getId(),
                        "state", gameManager.getStateDisplay(sender, arena.getState()),
                        "current", arena.getPlayerCount(),
                        "max", arena.getMaxPlayers()
                    );
                }
                return true;
            }
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    gameManager.sendLocalized(sender, "command.only_players");
                    return true;
                }
                gameManager.joinArena(player, args.length >= 2 ? args[1] : "random");
                return true;
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    gameManager.sendLocalized(sender, "command.only_players");
                    return true;
                }
                gameManager.leaveArena(player, "reason.command_leave");
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission("skyrandom.admin")) {
                    gameManager.sendLocalized(sender, "command.no_permission");
                    return true;
                }
                if (args.length < 2) {
                    gameManager.sendLocalized(sender, "command.usage_start", "label", label);
                    return true;
                }
                gameManager.forceStart(args[1], sender);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("skyrandom.admin")) {
                    gameManager.sendLocalized(sender, "command.no_permission");
                    return true;
                }
                gameManager.reload();
                gameManager.sendLocalized(sender, "command.reloaded");
                return true;
            }
            case "rebuildarena" -> {
                if (!sender.hasPermission("skyrandom.admin")) {
                    gameManager.sendLocalized(sender, "command.no_permission");
                    return true;
                }

                String arenaId = null;
                if (args.length >= 2) {
                    arenaId = args[1];
                } else if (sender instanceof Player player) {
                    Arena arena = gameManager.getArena(player);
                    if (arena != null) {
                        arenaId = arena.getId();
                    }
                }

                if (arenaId == null || arenaId.isBlank()) {
                    arenaId = gameManager.getDefaultArenaId();
                }

                if (arenaId == null || arenaId.isBlank()) {
                    gameManager.sendLocalized(sender, "command.usage_rebuild", "label", label);
                    return true;
                }

                gameManager.rebuildArena(arenaId, sender);
                return true;
            }
            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("join", "leave", "list", "start", "reload", "rebuildarena"), args[0]);
        }

        if (args.length == 2 && ("join".equalsIgnoreCase(args[0]) || "start".equalsIgnoreCase(args[0]) || "rebuildarena".equalsIgnoreCase(args[0]))) {
            List<String> arenaIds = new ArrayList<>(gameManager.getArenaIds());
            if ("join".equalsIgnoreCase(args[0])) {
                arenaIds.add("random");
            }
            return filter(arenaIds, args[1]);
        }

        return List.of();
    }

    private void sendHelp(CommandSender sender, String label) {
        gameManager.sendLocalized(sender, "command.help_header");
        gameManager.sendLocalized(sender, "command.help_list", "label", label);
        gameManager.sendLocalized(sender, "command.help_join", "label", label);
        gameManager.sendLocalized(sender, "command.help_leave", "label", label);
        gameManager.sendLocalized(sender, "command.help_start", "label", label);
        gameManager.sendLocalized(sender, "command.help_reload", "label", label);
        gameManager.sendLocalized(sender, "command.help_rebuild", "label", label);
    }

    private List<String> filter(List<String> values, String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
            .sorted()
            .toList();
    }
}
