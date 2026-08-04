package dev.ro161012.killtoken;

import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
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
 *   <li>{@code /killtoken give [player] [amount]} &mdash; grants tokens
 *       directly, e.g. for rewards or manual payouts.</li>
 *   <li>{@code /killtoken giveblock [player] [amount]} &mdash; grants
 *       compressed Kill Token blocks (each worth 64 tokens).</li>
 *   <li>{@code /killtoken reload} &mdash; reloads {@code config.yml}.</li>
 * </ul>
 *
 * <p>Subcommands are guarded by the fine-grained permissions
 * {@code killtoken.set}, {@code killtoken.give} and {@code killtoken.reload},
 * all children of {@code killtoken.admin}.
 */
public final class KillTokenCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("set", "give", "giveblock", "reload");
    private static final List<String> AMOUNT_SUGGESTIONS = List.of("1", "16", "64");

    /** Hard cap for a single {@code /killtoken give} payout (36 stacks). */
    static final int MAX_GIVE_AMOUNT = 2304;

    /** Hard cap for a single {@code /killtoken giveblock} payout (9 stacks). */
    static final int MAX_BLOCK_GIVE_AMOUNT = 576;

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
            case "give" -> handleGive(sender, args);
            case "giveblock" -> handleGiveBlock(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void sendUsage(final CommandSender sender, final String label) {
        sender.sendMessage(KillTokenPlugin.color("&6KillToken &8| &7Commands:"));
        sender.sendMessage(KillTokenPlugin.color("&f/" + label + " set &8- &7use your main-hand item as the token"));
        sender.sendMessage(KillTokenPlugin.color(
                "&f/" + label + " give [player] [amount] &8- &7hand out tokens"));
        sender.sendMessage(KillTokenPlugin.color(
                "&f/" + label + " giveblock [player] [amount] &8- &7hand out compressed blocks"));
        sender.sendMessage(KillTokenPlugin.color("&f/" + label + " reload &8- &7reload the configuration"));
    }

    private void handleSet(final CommandSender sender) {
        if (!sender.hasPermission("killtoken.set")) {
            sender.sendMessage(KillTokenPlugin.color("&cYou do not have permission to do that."));
            return;
        }
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

    private void handleGive(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("killtoken.give")) {
            sender.sendMessage(KillTokenPlugin.color("&cYou do not have permission to do that."));
            return;
        }

        final Player target;
        int amount = 1;

        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(KillTokenPlugin.color("&cPlayer &f" + args[1] + "&c is not online."));
                return;
            }
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(KillTokenPlugin.color("&cAmount must be a whole number."));
                    return;
                }
                if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
                    sender.sendMessage(KillTokenPlugin.color(
                            "&cAmount must be between &f1&c and &f" + MAX_GIVE_AMOUNT + "&c."));
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(KillTokenPlugin.color(
                        "&cUsage: /killtoken give <player> [amount]"));
                return;
            }
            target = player;
        }

        deliver(target, amount);
        sender.sendMessage(KillTokenPlugin.color("&aGave &f" + amount
                + " Kill Token" + (amount == 1 ? "" : "s") + "&a to &f" + target.getName() + "&a."));
    }

    /**
     * Spawns the tokens on the floor at the target's feet. Tokens are never
     * placed directly into an inventory - they are always physical drops.
     *
     * @param target receiving player
     * @param amount number of tokens to hand out
     */
    private void deliver(final Player target, final int amount) {
        target.getWorld().dropItemNaturally(target.getLocation(), plugin.createToken(amount));
    }

    private void handleGiveBlock(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("killtoken.give")) {
            sender.sendMessage(KillTokenPlugin.color("&cYou do not have permission to do that."));
            return;
        }

        final Player target;
        int amount = 1;

        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(KillTokenPlugin.color("&cPlayer &f" + args[1] + "&c is not online."));
                return;
            }
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(KillTokenPlugin.color("&cAmount must be a whole number."));
                    return;
                }
                if (amount < 1 || amount > MAX_BLOCK_GIVE_AMOUNT) {
                    sender.sendMessage(KillTokenPlugin.color("&cAmount must be between &f1&c and &f"
                            + MAX_BLOCK_GIVE_AMOUNT + "&c."));
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(KillTokenPlugin.color(
                        "&cUsage: /killtoken giveblock <player> [amount]"));
                return;
            }
            target = player;
        }

        final ItemStack stack = plugin.getCompressedBlockManager().createCompressedBlock();
        stack.setAmount(amount);
        target.getWorld().dropItemNaturally(target.getLocation(), stack);
        sender.sendMessage(KillTokenPlugin.color("&aGave &f" + amount + " Compressed Kill Token Block"
                + (amount == 1 ? "" : "s") + "&a to &f" + target.getName() + "&a."));
    }

    private void handleReload(final CommandSender sender) {
        if (!sender.hasPermission("killtoken.reload")) {
            sender.sendMessage(KillTokenPlugin.color("&cYou do not have permission to do that."));
            return;
        }
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
        if (args.length == 2 && (args[0].equalsIgnoreCase("give")
                || args[0].equalsIgnoreCase("giveblock"))) {
            final String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give")
                || args[0].equalsIgnoreCase("giveblock"))) {
            return AMOUNT_SUGGESTIONS;
        }
        return List.of();
    }
}
