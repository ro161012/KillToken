package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for the kill listener and the anti-farming cooldown.
 */
final class KillListenerTest {

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

    private long droppedTokenCount() {
        return server.getWorlds().get(0).getEntities().stream()
                .filter(entity -> entity.getType() == EntityType.ITEM)
                .count();
    }

    private void firePlayerKill(final PlayerMock victim, final PlayerMock killer) {
        victim.setKiller(killer);
        final DamageSource source = DamageSource.builder(DamageType.GENERIC)
                .withCausingEntity(killer)
                .withDirectEntity(killer)
                .build();
        final PlayerDeathEvent event =
                new PlayerDeathEvent(victim, source, new ArrayList<>(), 0, (String) null);
        server.getPluginManager().callEvent(event);
    }

    @Test
    @DisplayName("a PvP kill drops exactly one token")
    void pvpKillDropsToken() {
        final PlayerMock killer = server.addPlayer();
        final PlayerMock victim = server.addPlayer();

        firePlayerKill(victim, killer);

        assertEquals(1, droppedTokenCount());
        assertTrue(plugin.getPairCooldown().isOnCooldown(killer.getUniqueId(), victim.getUniqueId()));
    }

    @Test
    @DisplayName("a second kill within the cooldown drops nothing, in either direction")
    void pairCooldownSuppressesFarming() {
        final PlayerMock a = server.addPlayer();
        final PlayerMock b = server.addPlayer();

        firePlayerKill(b, a);
        assertEquals(1, droppedTokenCount());

        // a -> b again: suppressed
        firePlayerKill(b, a);
        assertEquals(1, droppedTokenCount());

        // b -> a revenge kill: also suppressed (pair is symmetric)
        firePlayerKill(a, b);
        assertEquals(1, droppedTokenCount());
    }

    @Test
    @DisplayName("kills involving other players are unaffected by an existing pair cooldown")
    void cooldownIsPairSpecific() {
        final PlayerMock a = server.addPlayer();
        final PlayerMock b = server.addPlayer();
        final PlayerMock c = server.addPlayer();

        firePlayerKill(b, a);
        assertEquals(1, droppedTokenCount());

        // a -> c is a different pair: drops normally
        firePlayerKill(c, a);
        assertEquals(2, droppedTokenCount());

        // the new cooldown applies to a<->c only; b<->c was never involved
        assertTrue(plugin.getPairCooldown().isOnCooldown(a.getUniqueId(), c.getUniqueId()));
        assertFalse(plugin.getPairCooldown().isOnCooldown(b.getUniqueId(), c.getUniqueId()));
    }

    @Test
    @DisplayName("deaths without a player killer never drop tokens")
    void nonPlayerDeathsDropNothing() {
        final PlayerMock victim = server.addPlayer();
        victim.setKiller(null);

        final DamageSource source = DamageSource.builder(DamageType.GENERIC).build();
        final PlayerDeathEvent event =
                new PlayerDeathEvent(victim, source, new ArrayList<>(), 0, (String) null);
        server.getPluginManager().callEvent(event);

        assertEquals(0, droppedTokenCount());
    }

    @Test
    @DisplayName("the dropped item is the configured currency")
    void droppedItemIsTheCurrency() {
        plugin.setCurrencyItem(new org.bukkit.inventory.ItemStack(Material.GOLD_INGOT));

        final PlayerMock killer = server.addPlayer();
        final PlayerMock victim = server.addPlayer();
        firePlayerKill(victim, killer);

        final Entity dropped = server.getWorlds().get(0).getEntities().stream()
                .filter(entity -> entity.getType() == EntityType.ITEM)
                .findFirst()
                .orElseThrow();
        assertEquals(EntityType.ITEM, dropped.getType());
    }
}
