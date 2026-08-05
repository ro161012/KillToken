package dev.ro161012.killtoken;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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
 *
 * <p>All configuration-derived values are resolved once when the config is
 * loaded or reloaded and cached in fields, so hot paths (death events,
 * commands) never re-parse YAML, enum names or color codes.
 */
public class KillTokenPlugin extends JavaPlugin {

    /** Configuration path of the serialized currency item. */
    public static final String CURRENCY_PATH = "currency-item";

    private static final String DEFAULT_TOKEN_NAME = "Kill Token";
    private static final String DEFAULT_TOKEN_LORE = "Awarded for killing another player.";
    private static final String LEGACY_DEFAULT_TOKEN_LORE = "Awarded for slaying another player.";
    private static final String DEFAULT_KILLSTREAK_MESSAGE = "&c%player% &7is on a &6%streak% &7killstreak!";
    private static final String LEGACY_KILLSTREAK_MESSAGE = "&6Killstreak&8: &f%streak%";
    private static final int MAX_KILLSTREAK_REWARD_TOKENS = 2304;

    private PairCooldown pairCooldown;
    private KillstreakTracker killstreakTracker;
    private CompressedBlockManager compressedBlocks;
    private ItemStack currencyItem;

    // Cached configuration values (refreshed by refreshConfigCache()).
    private int tokensPerKill;
    private long cooldownSeconds;
    private boolean notifyOnCooldown;
    private String cooldownMessage;
    private String killMessage;
    private boolean killstreakEnabled;
    private String killstreakMessage;
    private Sound killstreakSound;
    private int killstreakRewardEvery;
    private int killstreakRewardTokens;
    private String killstreakRewardMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        refreshConfigCache();

        this.pairCooldown = new PairCooldown(cooldownSeconds);
        this.killstreakTracker = new KillstreakTracker(this);
        this.compressedBlocks = new CompressedBlockManager(this);
        loadCurrencyItem();
        compressedBlocks.refresh();

        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new CompressedBlockListener(), this);

        final PluginCommand command = getCommand("killtoken");
        if (command != null) {
            final KillTokenCommand executor = new KillTokenCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("KillToken enabled (currency: " + currencyItem.getType()
                + ", pair cooldown: " + cooldownSeconds + "s).");
    }

    @Override
    public void onDisable() {
        getLogger().info("KillToken disabled.");
    }

    /**
     * Reloads configuration values from disk. The currency item is re-read
     * and the cached configuration values are refreshed.
     */
    public void reload() {
        reloadConfig();
        applyConfig();
        getLogger().info("Configuration reloaded.");
    }

    /**
     * Re-applies the current in-memory configuration to runtime state: the
     * currency item, the cached configuration values, the pair cooldown
     * duration and the compressed block template are refreshed.
     */
    public void applyConfig() {
        loadCurrencyItem();
        refreshConfigCache();
        pairCooldown.setCooldownSeconds(cooldownSeconds);
        compressedBlocks.refresh();
    }

    /**
     * Loads the currency item from {@code config.yml}, seeding the default
     * Nether Star token on first startup.
     */
    private void loadCurrencyItem() {
        final FileConfiguration config = getConfig();
        final ItemStack stored = config.getItemStack(CURRENCY_PATH);
        if (stored != null) {
            if (isLegacyDefaultToken(stored)) {
                this.currencyItem = createDefaultToken();
                config.set(CURRENCY_PATH, currencyItem);
                saveConfig();
                getLogger().info("Updated the default Kill Token lore.");
            } else {
                this.currencyItem = stored.clone();
            }
            return;
        }
        this.currencyItem = createDefaultToken();
        config.set(CURRENCY_PATH, currencyItem);
        saveConfig();
        getLogger().fine("Seeded default currency item (NETHER_STAR).");
    }

    /**
     * Reads every config value used at runtime into fields, so hot paths
     * only touch fields. Called on enable and on every reload.
     */
    private void refreshConfigCache() {
        final FileConfiguration config = getConfig();
        migrateKillstreakConfig(config);

        this.tokensPerKill = Math.max(1, config.getInt("tokens-per-kill", 1));
        this.cooldownSeconds = config.getLong("cooldown-seconds", 60L);
        this.notifyOnCooldown = config.getBoolean("notify-on-cooldown", true);
        this.cooldownMessage = color(config.getString("cooldown-message",
                "&cNo Kill Token dropped - you and this player are on cooldown."));
        this.killMessage = color(config.getString("kill-message", "&6+1 Kill Token"));

        this.killstreakEnabled = config.getBoolean("killstreak.enabled", true);
        this.killstreakMessage = color(config.getString("killstreak.message",
                DEFAULT_KILLSTREAK_MESSAGE));
        final String soundName = config.getString(
                "killstreak.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            this.killstreakSound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            this.killstreakSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        this.killstreakRewardEvery = Math.max(1, config.getInt("killstreak.reward-every", 3));
        this.killstreakRewardTokens = Math.min(MAX_KILLSTREAK_REWARD_TOKENS,
                Math.max(1, config.getInt("killstreak.reward-tokens", 2)));
        this.killstreakRewardMessage = color(config.getString("killstreak.reward-message",
                "&6+%amount% Kill Tokens &7for your &c%streak% &7killstreak!"));
    }

    /**
     * Updates the old stock action-bar template and adds new reward settings
     * to existing server configurations. Administrator-customized messages
     * are preserved.
     *
     * @param config current plugin configuration
     */
    private void migrateKillstreakConfig(final FileConfiguration config) {
        boolean changed = false;
        if (LEGACY_KILLSTREAK_MESSAGE.equals(config.getString("killstreak.message"))) {
            config.set("killstreak.message", DEFAULT_KILLSTREAK_MESSAGE);
            changed = true;
        }
        if (!config.contains("killstreak.reward-every")) {
            config.set("killstreak.reward-every", 3);
            changed = true;
        }
        if (!config.contains("killstreak.reward-tokens")) {
            config.set("killstreak.reward-tokens", 2);
            changed = true;
        }
        if (!config.contains("killstreak.reward-message")) {
            config.set("killstreak.reward-message",
                    "&6+%amount% Kill Tokens &7for your &c%streak% &7killstreak!");
            changed = true;
        }
        if (changed) {
            saveConfig();
        }
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
            meta.setDisplayName(color("&6" + DEFAULT_TOKEN_NAME));
            meta.setLore(List.of(color("&7" + DEFAULT_TOKEN_LORE)));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Returns whether an item is the v1.2.2-or-earlier default token.
     *
     * <p>Only this exact stock item is migrated. Administrator-created
     * currency items, including custom Nether Stars, remain untouched.
     *
     * @param stack item read from configuration
     * @return {@code true} when the item has the old default lore
     */
    private boolean isLegacyDefaultToken(ItemStack stack) {
        if (stack.getType() != Material.NETHER_STAR || !stack.hasItemMeta()) {
            return false;
        }

        final ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.hasLore() || meta.getLore() == null
                || meta.getLore().size() != 1) {
            return false;
        }

        return DEFAULT_TOKEN_NAME.equals(ChatColor.stripColor(meta.getDisplayName()))
                && LEGACY_DEFAULT_TOKEN_LORE.equals(ChatColor.stripColor(meta.getLore().get(0)));
    }

    /**
     * Creates a fresh copy of the currency item with the configured drop
     * amount, ready to be spawned into a world.
     *
     * @return the token item stack to drop
     */
    public ItemStack createToken() {
        return createToken(tokensPerKill);
    }

    /**
     * Creates a fresh copy of the currency item with the given amount.
     *
     * @param amount stack size of the returned item (clamped to at least 1)
     * @return the token item stack
     */
    public ItemStack createToken(final int amount) {
        final ItemStack stack = currencyItem.clone();
        stack.setAmount(Math.max(1, amount));
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
     * Returns the killstreak tracker.
     *
     * @return the killstreak tracker
     */
    public KillstreakTracker getKillstreakTracker() {
        return killstreakTracker;
    }

    /**
     * Returns the compressed block manager.
     *
     * @return the compressed block manager
     */
    public CompressedBlockManager getCompressedBlockManager() {
        return compressedBlocks;
    }

    /**
     * Whether killstreak chat announcements, personal sounds, and milestone
     * rewards are enabled.
     *
     * @return true if enabled
     */
    public boolean killstreakEnabled() {
        return killstreakEnabled;
    }

    /**
     * Returns the colourised killstreak chat message with the {@code %player%}
     * and {@code %streak%} placeholders.
     *
     * @return message template
     */
    public String getKillstreakMessage() {
        return killstreakMessage;
    }

    /**
     * Returns the configured killstreak sound, resolved once at config load.
     * The sound is always played at Minecraft's normal pitch of 1.0.
     *
     * @return the sound to play
     */
    public Sound getKillstreakSound() {
        return killstreakSound;
    }

    /**
     * Returns whether the supplied streak has earned a reward milestone.
     *
     * @param streak current streak length
     * @return true when rewards are enabled and the streak is a milestone
     */
    public boolean shouldRewardKillstreak(final int streak) {
        return killstreakEnabled && streak > 0 && streak % killstreakRewardEvery == 0;
    }

    /**
     * Gives the configured streak reward directly to the player's inventory.
     * Any amount that does not fit is dropped at the player's feet.
     *
     * @param player player receiving the reward
     * @param streak streak length that earned the reward
     */
    public void rewardKillstreak(final Player player, final int streak) {
        final Map<Integer, ItemStack> leftover = player.getInventory().addItem(
                createToken(killstreakRewardTokens));
        for (final ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        if (!killstreakRewardMessage.isEmpty()) {
            player.sendMessage(killstreakRewardMessage
                    .replace("%amount%", String.valueOf(killstreakRewardTokens))
                    .replace("%streak%", String.valueOf(streak)));
        }
    }

    /**
     * Runs a safe preview of the configured reward milestone for an
     * administrator. It broadcasts the chat announcement, plays the personal
     * sound, and gives the configured bonus without changing a real streak,
     * pair cooldown, or normal kill-token drop.
     *
     * @param player administrator running the preview
     * @return false when killstreaks are disabled
     */
    public boolean runKillstreakTest(final Player player) {
        if (!killstreakEnabled) {
            return false;
        }

        killstreakTracker.preview(player, killstreakRewardEvery);
        rewardKillstreak(player, killstreakRewardEvery);
        return true;
    }

    /**
     * Returns the number of qualifying kills between rewards.
     *
     * @return reward interval, always at least one
     */
    public int getKillstreakRewardEvery() {
        return killstreakRewardEvery;
    }

    /**
     * Returns the number of Kill Tokens given at a reward milestone.
     *
     * @return reward amount, between one and the configured safety cap
     */
    public int getKillstreakRewardTokens() {
        return killstreakRewardTokens;
    }

    /**
     * Returns the configured pair cooldown length in seconds.
     *
     * @return cooldown length in seconds
     */
    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Returns the configured number of tokens dropped per qualifying kill.
     *
     * @return tokens per kill (always at least 1)
     */
    public int getTokensPerKill() {
        return tokensPerKill;
    }

    /**
     * Whether the killer should be notified when a drop is suppressed by the
     * pair cooldown.
     *
     * @return true if the notification is enabled
     */
    public boolean notifyOnCooldown() {
        return notifyOnCooldown;
    }

    /**
     * Returns the colourised cooldown notification message.
     *
     * @return cooldown message
     */
    public String getCooldownMessage() {
        return cooldownMessage;
    }

    /**
     * Returns the colourised message sent to the killer on a successful drop.
     * May be empty if disabled in the configuration.
     *
     * @return kill message
     */
    public String getKillMessage() {
        return killMessage;
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
