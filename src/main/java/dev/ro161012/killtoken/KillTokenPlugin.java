package dev.ro161012.killtoken;

import java.util.List;
import java.util.Locale;

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
    private static final String DEFAULT_KILLSTREAK_MESSAGE = "&c%player% is on a %streak% killstreak!";
    private static final String LEGACY_KILLSTREAK_MESSAGE = "&6Killstreak&8: &f%streak%";
    private static final String PREVIOUS_NEUTRAL_KILLSTREAK_MESSAGE = "&c%player% &7is on a &6%streak% &7killstreak!";
    private static final String PREVIOUS_WARM_KILLSTREAK_MESSAGE = "&c%player% &6is on a &e%streak% &ekillstreak!";
    private static final int MAX_KILLSTREAK_TOKEN_MULTIPLIER = 5;

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
    private int killstreakAnnouncementMinimum;
    private int killstreakRewardStart;
    private int killstreakRewardStep;
    private int killstreakMaxTokenMultiplier;

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
        this.killstreakAnnouncementMinimum = Math.max(1,
                config.getInt("killstreak.announcement-minimum", 2));
        this.killstreakRewardStart = Math.max(1, config.getInt("killstreak.reward-start", 3));
        this.killstreakRewardStep = Math.max(1, config.getInt("killstreak.reward-step", 3));
        this.killstreakMaxTokenMultiplier = Math.min(MAX_KILLSTREAK_TOKEN_MULTIPLIER,
                Math.max(1, config.getInt("killstreak.max-token-multiplier", 5)));
    }

    /**
     * Updates the old stock action-bar template and adds streak multiplier
     * settings to existing server configurations. Administrator-customized
     * messages are preserved.
     *
     * @param config current plugin configuration
     */
    private void migrateKillstreakConfig(final FileConfiguration config) {
        boolean changed = false;
        final String configuredMessage = config.getString("killstreak.message");
        if (LEGACY_KILLSTREAK_MESSAGE.equals(configuredMessage)
                || PREVIOUS_NEUTRAL_KILLSTREAK_MESSAGE.equals(configuredMessage)
                || PREVIOUS_WARM_KILLSTREAK_MESSAGE.equals(configuredMessage)) {
            config.set("killstreak.message", DEFAULT_KILLSTREAK_MESSAGE);
            changed = true;
        }
        if (!config.contains("killstreak.announcement-minimum")) {
            config.set("killstreak.announcement-minimum", 2);
            changed = true;
        }
        if (!config.contains("killstreak.reward-start")) {
            config.set("killstreak.reward-start", 3);
            changed = true;
        }
        if (!config.contains("killstreak.reward-step")) {
            config.set("killstreak.reward-step", config.getInt("killstreak.reward-every", 3));
            changed = true;
        }
        if (!config.contains("killstreak.max-token-multiplier")) {
            config.set("killstreak.max-token-multiplier", 5);
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
     * Whether killstreak chat announcements, personal sounds, and token
     * multipliers are enabled.
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
     * Returns whether a streak should be announced in chat and with a sound.
     *
     * @param streak current streak length
     * @return true when announcements are enabled at this streak length
     */
    public boolean shouldAnnounceKillstreak(final int streak) {
        return killstreakEnabled && streak >= killstreakAnnouncementMinimum;
    }

    /**
     * Returns the Kill Token multiplier for a streak. The multiplier starts
     * at two on the configured reward-start streak and increases by one every
     * reward-step kills, capped at the configured maximum.
     *
     * @param streak current streak length
     * @return multiplier for the normal token drop, at least one
     */
    public int getKillstreakTokenMultiplier(final int streak) {
        if (!killstreakEnabled || streak < killstreakRewardStart) {
            return 1;
        }

        final int multiplier = 2 + (streak - killstreakRewardStart) / killstreakRewardStep;
        return Math.min(killstreakMaxTokenMultiplier, multiplier);
    }

    /**
     * Returns the token amount for a qualifying kill at the supplied streak.
     *
     * @param streak current streak length
     * @return normal configured drop amount multiplied by the streak multiplier
     */
    public int getKillstreakTokenAmount(final int streak) {
        return tokensPerKill * getKillstreakTokenMultiplier(streak);
    }

    /**
     * Runs a safe preview of the chat announcements and multiplier drop for
     * an administrator. It does not change a real streak or pair cooldown.
     *
     * @param player administrator running the preview
     * @return false when killstreaks are disabled
     */
    public boolean runKillstreakTest(final Player player) {
        if (!killstreakEnabled) {
            return false;
        }

        killstreakTracker.preview(player, killstreakAnnouncementMinimum);
        if (killstreakRewardStart != killstreakAnnouncementMinimum) {
            killstreakTracker.preview(player, killstreakRewardStart);
        }
        player.getWorld().dropItemNaturally(player.getLocation(),
                createToken(getKillstreakTokenAmount(killstreakRewardStart)));
        return true;
    }

    /**
     * Returns the minimum streak announced to chat.
     *
     * @return announcement minimum, always at least one
     */
    public int getKillstreakAnnouncementMinimum() {
        return killstreakAnnouncementMinimum;
    }

    /**
     * Returns the streak where token multiplication starts.
     *
     * @return reward start, always at least one
     */
    public int getKillstreakRewardStart() {
        return killstreakRewardStart;
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
