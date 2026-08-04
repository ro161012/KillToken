package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Tests for the killstreak counter and its progressive sound pitch.
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
    @DisplayName("pitch rises with the streak and is clamped at the configured maximum")
    void pitchIsProgressiveAndClamped() {
        final KillstreakTracker tracker = plugin.getKillstreakTracker();
        // defaults: base 0.7, +0.15 per kill, max 2.0

        final float first = tracker.pitchFor(1);
        final float second = tracker.pitchFor(2);
        final float fifth = tracker.pitchFor(5);

        assertTrue(second > first, "pitch must rise with the streak");
        assertTrue(fifth > second, "pitch must keep rising");
        assertEquals(0.7f, first, 0.001f);
        assertEquals(2.0f, tracker.pitchFor(100), 0.001f, "pitch is capped at max-pitch");
    }

    @Test
    @DisplayName("pitch never drops below the playable minimum")
    void pitchIsBoundedAtTheMinimum() {
        plugin.getConfig().set("killstreak.base-pitch", 0.1);
        plugin.applyConfig();

        assertEquals(KillstreakTracker.MIN_PITCH,
                plugin.getKillstreakTracker().pitchFor(1), 0.001f);
    }

    @Test
    @DisplayName("disabled killstreaks still count kills but announce nothing")
    void disabledKillstreakStillCounts() {
        plugin.getConfig().set("killstreak.enabled", false);
        final PlayerMock player = server.addPlayer();

        assertEquals(1, plugin.getKillstreakTracker().increment(player));
    }
}
