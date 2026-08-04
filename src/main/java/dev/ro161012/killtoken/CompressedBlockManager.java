package dev.ro161012.killtoken;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Storage block for the Kill Token currency.
 *
 * <p>A single Compressed Kill Token Block holds {@value #COMPRESS_RATIO}
 * Kill Tokens. It uses a quartz-style material so it reads as
 * "token-like" at a glance, and its lore states the stored value.
 *
 * <p>No crafting recipes are provided: servers wire the block into their
 * own trading systems (e.g. custom villager trades).
 */
public final class CompressedBlockManager {

    /** Tokens stored in one Compressed Kill Token Block. */
    public static final int COMPRESS_RATIO = 64;

    /** Configuration section for the compressed block feature. */
    public static final String CONFIG_SECTION = "compressed-blocks";

    private final KillTokenPlugin plugin;

    /**
     * Creates the manager for the owning plugin.
     *
     * @param plugin owning plugin instance
     */
    public CompressedBlockManager(final KillTokenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a Compressed Kill Token Block holding
     * {@value #COMPRESS_RATIO} tokens.
     *
     * @return the compressed block item
     */
    public ItemStack createCompressedBlock() {
        final ItemStack stack = new ItemStack(
                material("compressed-block-material", Material.QUARTZ_BLOCK));
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(KillTokenPlugin.color("&6Compressed Kill Token Block"));
            meta.setLore(List.of(
                    KillTokenPlugin.color("&7A compact block of Kill Tokens."),
                    KillTokenPlugin.color("&8Value: &e" + COMPRESS_RATIO + " &fKill Tokens")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Resolves a configurable material name, falling back to the given
     * default and warning on invalid values.
     *
     * @param path configuration path below {@value #CONFIG_SECTION}
     * @param fallback material used when the value is missing or unknown
     * @return the configured material
     */
    private Material material(final String path, final Material fallback) {
        final String name = plugin.getConfig()
                .getString(CONFIG_SECTION + "." + path, fallback.name());
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material '" + name
                    + "' for " + CONFIG_SECTION + "." + path + ", using "
                    + fallback.name() + ".");
            return fallback;
        }
    }
}
