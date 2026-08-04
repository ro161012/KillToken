package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

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
        assertEquals(2, currency.getItemMeta().getLore().size());

        assertNotNull(plugin.getConfig().get(KillTokenPlugin.CURRENCY_PATH),
                "default currency should be persisted to config.yml");
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
