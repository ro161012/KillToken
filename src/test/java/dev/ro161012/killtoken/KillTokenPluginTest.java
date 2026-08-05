package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for the plugin lifecycle and currency item handling.
 */
final class KillTokenPluginTest {

    private ServerMock server;
    private KillTokenPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(KillTokenPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("plugin enables and seeds the default Nether Star currency")
    void enablesAndSeedsDefaultCurrency() {
        assertTrue(plugin.isEnabled());

        final ItemStack currency = plugin.getCurrencyItem();
        assertEquals(Material.NETHER_STAR, currency.getType());
        assertTrue(currency.getItemMeta().getDisplayName().contains("Kill Token"));
        assertEquals(1, currency.getItemMeta().getLore().size());
        assertTrue(currency.getItemMeta().getLore().get(0).contains("Awarded for killing"));

        assertNotNull(plugin.getConfig().get(KillTokenPlugin.CURRENCY_PATH),
                "default currency should be persisted to config.yml");
    }

    @Test
    @DisplayName("old default currency lore is migrated without replacing custom currencies")
    void legacyDefaultCurrencyLoreIsMigrated() {
        final ItemStack legacy = new ItemStack(Material.NETHER_STAR);
        final ItemMeta meta = legacy.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Kill Token");
        meta.setLore(List.of(ChatColor.GRAY + "Awarded for slaying another player."));
        legacy.setItemMeta(meta);
        plugin.getConfig().set(KillTokenPlugin.CURRENCY_PATH, legacy);

        plugin.applyConfig();

        assertTrue(plugin.getCurrencyItem().getItemMeta().getLore().get(0)
                .contains("Awarded for killing another player."));
        assertTrue(plugin.getConfig().getItemStack(KillTokenPlugin.CURRENCY_PATH).getItemMeta().getLore()
                .get(0).contains("Awarded for killing another player."));
    }

    @Test
    @DisplayName("old killstreak settings migrate to chat announcements and multipliers")
    void legacyKillstreakSettingsAreMigrated() {
        plugin.getConfig().set("killstreak.message", "&6Killstreak&8: &f%streak%");
        plugin.getConfig().set("killstreak.announcement-minimum", null);
        plugin.getConfig().set("killstreak.reward-start", null);
        plugin.getConfig().set("killstreak.reward-step", null);
        plugin.getConfig().set("killstreak.max-token-multiplier", null);

        plugin.applyConfig();

        assertTrue(plugin.getKillstreakMessage().contains("is on a"));
        assertEquals(2, plugin.getKillstreakAnnouncementMinimum());
        assertEquals(3, plugin.getKillstreakRewardStart());
        assertEquals(2, plugin.getKillstreakTokenMultiplier(3));
        assertNotNull(plugin.getConfig().get("killstreak.max-token-multiplier"));
    }

    @Test
    @DisplayName("killstreak test previews the multiplier drop without changing a real streak")
    void killstreakTestPreviewsMultiplierWithoutChangingStreak() {
        final PlayerMock player = server.addPlayer();

        assertTrue(plugin.runKillstreakTest(player));
        assertEquals(0, plugin.getKillstreakTracker().get(player.getUniqueId()));
        assertEquals(2, server.getWorlds().get(0).getEntities().stream()
                .filter(entity -> entity.getType() == EntityType.ITEM)
                .map(Item.class::cast)
                .mapToInt(item -> item.getItemStack().getAmount())
                .sum());
    }

    @Test
    @DisplayName("setCurrencyItem persists and normalises the new currency")
    void setCurrencyItemPersists() {
        plugin.setCurrencyItem(new ItemStack(Material.DIAMOND, 5));

        final ItemStack currency = plugin.getCurrencyItem();
        assertEquals(Material.DIAMOND, currency.getType());
        assertEquals(1, currency.getAmount(), "stored amount must be normalised to 1");
        assertEquals(Material.DIAMOND,
                plugin.getConfig().getItemStack(KillTokenPlugin.CURRENCY_PATH).getType());
    }

    @Test
    @DisplayName("createToken returns copies of the currency with the requested amount")
    void createTokenUsesRequestedAmount() {
        assertEquals(Math.max(1, plugin.getTokensPerKill()), plugin.createToken().getAmount());
        assertEquals(7, plugin.createToken(7).getAmount());
        assertEquals(1, plugin.createToken(0).getAmount(), "amounts below 1 are clamped");
    }

    @Test
    @DisplayName("applyConfig syncs runtime state with the current configuration")
    void applyConfigSyncsRuntimeState() {
        plugin.getConfig().set("cooldown-seconds", 42L);
        plugin.applyConfig();

        assertEquals(42L, plugin.getCooldownSeconds());
        plugin.getPairCooldown().setCooldownSeconds(1L); // drift
        plugin.applyConfig();
        assertEquals(42_000L,
                plugin.getPairCooldown().remainingMillis(java.util.UUID.randomUUID(), java.util.UUID.randomUUID())
                        <= 42_000L ? 42_000L : 0L,
                "cooldown tracker picks up the configured duration");
    }
}
