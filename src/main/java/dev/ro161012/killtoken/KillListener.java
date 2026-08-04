package dev.ro161012.killtoken;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Spawns the Kill Token when a player is killed by another player, subject
 * to the pair-based anti-farming cooldown.
 */
public final class KillListener implements Listener {

    private final KillTokenPlugin plugin;

    /**
     * Creates the listener.
     *
     * @param plugin owning plugin instance
     */
    public KillListener(final KillTokenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player deaths. A token is dropped only for player-versus-player
     * kills that are not suppressed by the pair cooldown.
     *
     * @param event the death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player victim = event.getEntity();
        final Player killer = victim.getKiller();

        // Only player-versus-player kills award tokens.
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        final PairCooldown cooldown = plugin.getPairCooldown();

        // Anti-farming: the exact killer/victim pair (in either direction)
        // cannot generate tokens again until the cooldown expires.
        if (cooldown.isOnCooldown(killer.getUniqueId(), victim.getUniqueId())) {
            if (plugin.notifyOnCooldown()) {
                killer.sendMessage(plugin.getCooldownMessage());
            }
            return;
        }

        cooldown.apply(killer.getUniqueId(), victim.getUniqueId());

        final Location deathLocation = victim.getLocation();
        victim.getWorld().dropItemNaturally(deathLocation, plugin.createToken());

        final String killMessage = plugin.getKillMessage();
        if (!killMessage.isEmpty()) {
            killer.sendMessage(killMessage);
        }
    }
}
