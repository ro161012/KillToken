package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Tests for the killstreak counter and reward milestones.
 */
final class KillstreakTrackerTest {

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
    @DisplayName("increment counts consecutive kills per player")
    void incrementCountsConsecutiveKills() {
        final PlayerMock player = server.addPlayer();
        final KillstreakTracker tracker = plugin.getKillstreakTracker();

        assertEquals(1, tracker.increment(player));
        assertEquals(2, tracker.increment(player));
        assertEquals(3, tracker.increment(player));
        assertEquals(3, tracker.get(player.getUniqueId()));
    }

    @Test
    @DisplayName("streaks are tracked per player")
    void streaksArePerPlayer() {
        final PlayerMock a = server.addPlayer();
        final PlayerMock b = server.addPlayer();
        final KillstreakTracker tracker = plugin.getKillstreakTracker();

        tracker.increment(a);
        tracker.increment(a);
        tracker.increment(b);

        assertEquals(2, tracker.get(a.getUniqueId()));
        assertEquals(1, tracker.get(b.getUniqueId()));
    }

    @Test
    @DisplayName("reset clears the streak")
    void resetClearsStreak() {
        final PlayerMock player = server.addPlayer();
        final KillstreakTracker tracker = plugin.getKillstreakTracker();

        tracker.increment(player);
        tracker.reset(player.getUniqueId());

        assertEquals(0, tracker.get(player.getUniqueId()));
        assertEquals(1, tracker.increment(player), "a new streak starts at 1");
    }

    @Test
    @DisplayName("default reward milestones are every three qualifying kills")
    void rewardMilestonesUseDefaultInterval() {
        assertEquals(3, plugin.getKillstreakRewardEvery());
        assertEquals(2, plugin.getKillstreakRewardTokens());
        assertFalse(plugin.shouldRewardKillstreak(1));
        assertFalse(plugin.shouldRewardKillstreak(2));
        assertTrue(plugin.shouldRewardKillstreak(3));
        assertFalse(plugin.shouldRewardKillstreak(4));
        assertTrue(plugin.shouldRewardKillstreak(6));
    }

    @Test
    @DisplayName("disabled killstreaks still count kills but do not reward milestones")
    void disabledKillstreakStillCountsWithoutRewards() {
        plugin.getConfig().set("killstreak.enabled", false);
        plugin.applyConfig();
        final PlayerMock player = server.addPlayer();

        assertEquals(1, plugin.getKillstreakTracker().increment(player));
        assertFalse(plugin.shouldRewardKillstreak(3));
    }
}
