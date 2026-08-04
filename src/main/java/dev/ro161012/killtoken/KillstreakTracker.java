package dev.ro161012.killtoken;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Tracks consecutive PvP kills per player and presents them to the killer:
 * a short action-bar counter (the text above the hotbar, between the health
 * and hunger indicators) and a sound whose pitch rises with each consecutive
 * kill.
 *
 * <p>A streak ends when its owner dies - from any cause - or leaves the
 * server. Streaks are intentionally kept in memory only.
 */
public final class KillstreakTracker {

    /** Lowest pitch Minecraft plays back sensibly. */
    static final float MIN_PITCH = 0.5f;

    private static final float VOLUME = 1.0f;

    private final KillTokenPlugin plugin;
    private final Map<UUID, Integer> streaks = new ConcurrentHashMap<>();

    /**
     * Creates the tracker.
     *
     * @param plugin owning plugin instance
     */
    public KillstreakTracker(final KillTokenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Increments the killer's streak, announces it, and returns the new
     * streak length.
     *
     * @param killer the player who scored the kill
     * @return the new streak length
     */
    public int increment(final Player killer) {
        final int streak = streaks.merge(killer.getUniqueId(), 1, Integer::sum);
        announce(killer, streak);
        return streak;
    }

    /**
     * Clears a player's streak. Called whenever the player dies (from any
     * cause) or disconnects.
     *
     * @param playerId the player whose streak ends
     */
    public void reset(final UUID playerId) {
        streaks.remove(playerId);
    }

    /**
     * Returns the current streak length for a player.
     *
     * @param playerId the player
     * @return streak length, 0 if none
     */
    public int get(final UUID playerId) {
        return streaks.getOrDefault(playerId, 0);
    }

    /**
     * Computes the sound pitch for the given streak: the configured base
     * pitch plus one step per kill, clamped to
     * [{@value #MIN_PITCH}, max-pitch].
     *
     * @param streak current streak length (at least 1)
     * @return pitch to play the killstreak sound at
     */
    public float pitchFor(final int streak) {
        final float raw = plugin.getKillstreakBasePitch()
                + (streak - 1) * plugin.getKillstreakPitchPerKill();
        return Math.min(plugin.getKillstreakMaxPitch(), Math.max(MIN_PITCH, raw));
    }

    /**
     * Shows the action-bar message and plays the streak sound. The action
     * bar fades on its own after roughly a second of display time.
     */
    private void announce(final Player killer, final int streak) {
        if (!plugin.killstreakEnabled()) {
            return;
        }
        final String message = KillTokenPlugin.color(
                plugin.getKillstreakMessage().replace("%streak%", String.valueOf(streak)));
        final Component actionBar = LegacyComponentSerializer.legacySection().deserialize(message);
        killer.sendActionBar(actionBar);
        killer.playSound(killer.getLocation(), plugin.getKillstreakSound(), VOLUME, pitchFor(streak));
    }
}
