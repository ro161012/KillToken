package dev.ro161012.killtoken;

import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the KillToken plugin.
 *
 * <p>KillToken drops a configurable currency item whenever a player kills
 * another player, and applies a pair-based cooldown to prevent token farming
 * between the same two players.
 */
public final class KillTokenPlugin extends JavaPlugin {

    /** Configuration path of the serialized currency item. */
    public static final String CURRENCY_PATH = "currency-item";

    private PairCooldown pairCooldown;
    private ItemStack currencyItem;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pairCooldown = new PairCooldown(getCooldownSeconds());
        loadCurrencyItem();

        getServer().getPluginManager().registerEvents(new KillListener(this), this);

        final PluginCommand command = getCommand("killtoken");
        if (command != null) {
            final KillTokenCommand executor = new KillTokenCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("KillToken enabled (currency: " + currencyItem.getType()
                + ", pair cooldown: " + getCooldownSeconds() + "s).");
    }

    @Override
    public void onDisable() {
        getLogger().info("KillToken disabled.");
    }

    /**
     * Reloads configuration values from disk. The currency item is re-read
     * and the pair cooldown duration is updated.
     */
    public void reload() {
        reloadConfig();
        loadCurrencyItem();
        pairCooldown.setCooldownSeconds(getCooldownSeconds());
        getLogger().info("Configuration reloaded.");
    }

    /**
     * Loads the currency item from {@code config.yml}, seeding the default
     * Nether Star token on first startup.
     */
    private void loadCurrencyItem() {
        final FileConfiguration config = getConfig();
        final ItemStack stored = config.getItemStack(CURRENCY_PATH);
        if (stored != null) {
            this.currencyItem = stored.clone();
            return;
        }
        this.currencyItem = createDefaultToken();
        config.set(CURRENCY_PATH, currencyItem);
        saveConfig();
        getLogger().log(Level.FINE, "Seeded default currency item (NETHER_STAR).");
    }

    /**
     * Creates the default token: a Nether Star with a display name and lore.
     *
     * @return the default currency item
     */
    private ItemStack createDefaultToken() {
        final ItemStack stack = new ItemStack(Material.NETHER_STAR);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&6Kill Token"));
            meta.setLore(Arrays.asList(
                    color("&7Awarded for slaying another player."),
                    color("&7A rare currency on this server.")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Creates a fresh copy of the currency item with the configured drop
     * amount, ready to be spawned into a world.
     *
     * @return the token item stack to drop
     */
    public ItemStack createToken() {
        final ItemStack stack = currencyItem.clone();
        stack.setAmount(Math.max(1, getTokensPerKill()));
        return stack;
    }

    /**
     * Replaces the currency item and persists it to {@code config.yml}.
     *
     * @param stack the item to use as the new currency (amount is normalised to 1)
     */
    public void setCurrencyItem(final ItemStack stack) {
        final ItemStack copy = stack.clone();
        copy.setAmount(1);
        this.currencyItem = copy;
        getConfig().set(CURRENCY_PATH, copy);
        saveConfig();
    }

    /**
     * Returns a defensive copy of the current currency item.
     *
     * @return copy of the currency item
     */
    public ItemStack getCurrencyItem() {
        return currencyItem.clone();
    }

    /**
     * Returns the pair cooldown tracker.
     *
     * @return the cooldown tracker
     */
    public PairCooldown getPairCooldown() {
        return pairCooldown;
    }

    /**
     * Returns the configured pair cooldown length in seconds.
     *
     * @return cooldown length in seconds
     */
    public long getCooldownSeconds() {
        return getConfig().getLong("cooldown-seconds", 60L);
    }

    /**
     * Returns the configured number of tokens dropped per qualifying kill.
     *
     * @return tokens per kill (always at least 1 when used)
     */
    public int getTokensPerKill() {
        return getConfig().getInt("tokens-per-kill", 1);
    }

    /**
     * Whether the killer should be notified when a drop is suppressed by the
     * pair cooldown.
     *
     * @return true if the notification is enabled
     */
    public boolean notifyOnCooldown() {
        return getConfig().getBoolean("notify-on-cooldown", true);
    }

    /**
     * Returns the colourised cooldown notification message.
     *
     * @return cooldown message
     */
    public String getCooldownMessage() {
        return color(getConfig().getString("cooldown-message",
                "&cNo Kill Token dropped - you and this player are on cooldown."));
    }

    /**
     * Returns the colourised message sent to the killer on a successful drop.
     * May be empty if disabled in the configuration.
     *
     * @return kill message
     */
    public String getKillMessage() {
        return color(getConfig().getString("kill-message", "&6+1 Kill Token"));
    }

    /**
     * Translates {@code &}-style colour codes in the given string.
     *
     * @param value raw string, may be null
     * @return colourised string, never null
     */
    public static String color(final String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
