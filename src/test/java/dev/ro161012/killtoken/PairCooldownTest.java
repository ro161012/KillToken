package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the pair-based anti-farming cooldown.
 */
final class PairCooldownTest {

    @Test
    @DisplayName("A fresh pair is not on cooldown")
    void freshPairIsNotOnCooldown() {
        final PairCooldown cooldown = new PairCooldown(60);
        assertFalse(cooldown.isOnCooldown(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    @DisplayName("Applying a cooldown blocks both directions of the pair")
    void cooldownIsSymmetric() {
        final PairCooldown cooldown = new PairCooldown(60);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();

        cooldown.apply(a, b);

        assertTrue(cooldown.isOnCooldown(a, b), "a -> b should be blocked");
        assertTrue(cooldown.isOnCooldown(b, a), "b -> a should be blocked");
    }

    @Test
    @DisplayName("A cooldown does not affect kills involving other players")
    void cooldownIsPairSpecific() {
        final PairCooldown cooldown = new PairCooldown(60);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final UUID c = UUID.randomUUID();

        cooldown.apply(a, b);

        assertFalse(cooldown.isOnCooldown(a, c), "a -> c must not be blocked");
        assertFalse(cooldown.isOnCooldown(c, b), "c -> b must not be blocked");
    }

    @Test
    @DisplayName("Remaining time is reported and bounded at zero")
    void remainingTimeIsBounded() {
        final PairCooldown cooldown = new PairCooldown(60);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();

        assertEquals(0L, cooldown.remainingMillis(a, b));

        cooldown.apply(a, b);
        final long remaining = cooldown.remainingMillis(a, b);
        assertTrue(remaining > 0L && remaining <= 60_000L,
                "remaining time should be within the cooldown window, was " + remaining);
    }

    @Test
    @DisplayName("Expired cooldowns are treated as inactive")
    void expiredCooldownIsInactive() {
        final PairCooldown cooldown = new PairCooldown(0);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();

        cooldown.apply(a, b);

        assertFalse(cooldown.isOnCooldown(a, b), "a zero-second cooldown expires immediately");
        assertEquals(0L, cooldown.remainingMillis(a, b));
    }

    @Test
    @DisplayName("Re-applying a cooldown restarts the timer")
    void reapplyRestartsTimer() {
        final PairCooldown cooldown = new PairCooldown(60);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();

        cooldown.apply(a, b);
        final long first = cooldown.remainingMillis(a, b);
        cooldown.apply(a, b);
        final long second = cooldown.remainingMillis(a, b);

        assertTrue(second >= first - 50L, "re-applying must not shorten the cooldown");
    }

    @Test
    @DisplayName("purgeExpired removes stale entries only")
    void purgeExpiredRemovesStaleEntries() {
        final PairCooldown cooldown = new PairCooldown(0);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        cooldown.apply(a, b);

        cooldown.purgeExpired();

        assertFalse(cooldown.isOnCooldown(a, b));
    }
}
