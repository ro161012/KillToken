package dev.ro161012.killtoken;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

/**
 * Tracks consecutive PvP kills per player. Each qualifying kill broadcasts
 * the player's current streak in chat and plays a fixed-pitch sound only for
 * that player.
 *
 * <p>A streak ends when its owner dies - from any cause - or leaves the
 * server. Streaks are intentionally kept in memory only.
 */
public final class KillstreakTracker {

    private static final float VOLUME = 1.0f;
    private static final float SOUND_PITCH = 1.0f;

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
     * Increments the killer's streak, announces it in chat, and returns the
     * new streak length.
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
     * Shows a configured streak announcement without changing the player's
     * real streak. Used by the administrator test command.
     *
     * @param player player used in the preview
     * @param streak streak length to preview
     */
    public void preview(final Player player, final int streak) {
        announce(player, Math.max(1, streak));
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
     * Broadcasts the configured streak message and plays the configured sound
     * for the streak owner alone. The pitch stays at Minecraft's normal 1.0.
     *
     * @param killer player whose streak increased
     * @param streak current streak length
     */
    private void announce(final Player killer, final int streak) {
        if (!plugin.killstreakEnabled()) {
            return;
        }

        final String message = plugin.getKillstreakMessage()
                .replace("%player%", killer.getName())
                .replace("%streak%", String.valueOf(streak));
        plugin.getServer().broadcastMessage(message);
        killer.playSound(killer.getLocation(), plugin.getKillstreakSound(), VOLUME, SOUND_PITCH);
    }
}
