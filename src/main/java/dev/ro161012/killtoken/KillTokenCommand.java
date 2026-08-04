package dev.ro161012.killtoken;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Executor and tab-completer for {@code /killtoken}.
 *
 * <ul>
 *   <li>{@code /killtoken set} &mdash; the item in the sender's main hand
 *       becomes the new Kill Token currency item.</li>
 *   <li>{@code /killtoken reload} &mdash; reloads {@code config.yml}.</li>
 * </ul>
 */
public final class KillTokenCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("set", "reload");

    private final KillTokenPlugin plugin;

    /**
     * Creates the command handler.
     *
     * @param plugin owning plugin instance
     */
    public KillTokenCommand(final KillTokenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command,
                             final String label, final String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> handleSet(sender);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void sendUsage(final CommandSender sender, final String label) {
        sender.sendMessage(KillTokenPlugin.color("&6KillToken &8| &7Commands:"));
        sender.sendMessage(KillTokenPlugin.color("&f/" + label + " set &8- &7use your main-hand item as the token"));
        sender.sendMessage(KillTokenPlugin.color("&f/" + label + " reload &8- &7reload the configuration"));
    }

    private void handleSet(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(KillTokenPlugin.color("&cOnly players can set the Kill Token item."));
            return;
        }

        final ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage(KillTokenPlugin.color(
                    "&cHold the item you want to use as the Kill Token first."));
            return;
        }

        plugin.setCurrencyItem(held);
        player.sendMessage(KillTokenPlugin.color("&aKill Token currency updated to &f"
                + prettyName(held.getType()) + "&a."));
    }

    private void handleReload(final CommandSender sender) {
        plugin.reload();
        sender.sendMessage(KillTokenPlugin.color("&aKillToken configuration reloaded."));
    }

    private static String prettyName(final Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command,
                                      final String alias, final String[] args) {
        if (args.length == 1) {
            final String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(name -> name.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
