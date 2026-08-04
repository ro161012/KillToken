package dev.ro161012.killtoken;

import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Storage blocks for the Kill Token currency.
 *
 * <p>Nine Kill Tokens compress into a single Compressed Kill Token Block,
 * and nine of those into a Compressed Compressed Kill Token Block (81
 * tokens in total). Both blocks craft back into their components, so no
 * tokens are ever lost. The blocks use quartz-style materials so they
 * read as "token-like" at a glance.
 */
public final class CompressedBlockManager {

    /** Tokens stored in one Compressed Kill Token Block. */
    public static final int COMPRESS_RATIO = 9;

    /** Tokens stored in one Compressed Compressed Kill Token Block. */
    public static final int COMPRESSED_COMPRESSED_VALUE = COMPRESS_RATIO * COMPRESS_RATIO;

    /** Configuration section for the compressed block feature. */
    public static final String CONFIG_SECTION = "compressed-blocks";

    private static final String COMPRESSED_LORE_HEAD = "A dense slab of Kill Tokens, pressed";
    private static final String COMPRESSED_LORE_TAIL = "into a single portable block.";
    private static final String SUPER_LORE_HEAD = "Kill Tokens compressed to their";
    private static final String SUPER_LORE_TAIL = "absolute limit - a vault of carnage.";

    private final KillTokenPlugin plugin;
    private final NamespacedKey compressKey;
    private final NamespacedKey decompressKey;
    private final NamespacedKey compressCompressedKey;
    private final NamespacedKey decompressCompressedKey;

    private boolean recipesRegistered;

    /**
     * Creates the manager for the owning plugin.
     *
     * @param plugin owning plugin instance
     */
    public CompressedBlockManager(final KillTokenPlugin plugin) {
        this.plugin = plugin;
        this.compressKey = new NamespacedKey(plugin, "compress");
        this.decompressKey = new NamespacedKey(plugin, "decompress");
        this.compressCompressedKey = new NamespacedKey(plugin, "compress_compressed");
        this.decompressCompressedKey = new NamespacedKey(plugin, "decompress_compressed");
    }

    /**
     * Creates a Compressed Kill Token Block holding
     * {@value #COMPRESS_RATIO} tokens.
     *
     * @return the compressed block item
     */
    public ItemStack createCompressedBlock() {
        return build(material("compressed-block-material", Material.QUARTZ_BLOCK),
                "&6\u2726 Compressed Kill Token Block",
                List.of(
                        "&7" + COMPRESSED_LORE_HEAD,
                        "&7" + COMPRESSED_LORE_TAIL,
                        "&8Value&7: &e" + COMPRESS_RATIO + " &fKill Tokens",
                        "&8Uncrafts into " + COMPRESS_RATIO + " Kill Tokens"));
    }

    /**
     * Creates a Compressed Compressed Kill Token Block holding
     * {@value #COMPRESSED_COMPRESSED_VALUE} tokens.
     *
     * @return the compressed compressed block item
     */
    public ItemStack createCompressedCompressedBlock() {
        return build(material("compressed-compressed-block-material", Material.SMOOTH_QUARTZ),
                "&6\u2726\u2726 Compressed Compressed Kill Token Block",
                List.of(
                        "&7" + SUPER_LORE_HEAD,
                        "&7" + SUPER_LORE_TAIL,
                        "&8Value&7: &e" + COMPRESSED_COMPRESSED_VALUE + " &fKill Tokens",
                        "&8Uncrafts into " + COMPRESS_RATIO + " Compressed Blocks"));
    }

    /**
     * Registers the compression and decompression recipes, replacing any
     * previously registered ones. While the feature is disabled in the
     * configuration, all recipes are removed and nothing is registered.
     */
    public void registerRecipes() {
        unregisterRecipes();
        if (!plugin.getConfig().getBoolean(CONFIG_SECTION + ".enabled", true)) {
            return;
        }

        final ItemStack token = plugin.getCurrencyItem();
        final ItemStack compressed = createCompressedBlock();
        final ItemStack compressedCompressed = createCompressedCompressedBlock();

        registerCompress(compressKey, token, compressed);
        registerDecompress(decompressKey, compressed, token);
        registerCompress(compressCompressedKey, compressed, compressedCompressed);
        registerDecompress(decompressCompressedKey, compressedCompressed, compressed);
        recipesRegistered = true;
    }

    /**
     * Removes all recipes previously registered by this manager. Safe to
     * call multiple times.
     */
    public void unregisterRecipes() {
        if (!recipesRegistered) {
            return;
        }
        Bukkit.removeRecipe(compressKey);
        Bukkit.removeRecipe(decompressKey);
        Bukkit.removeRecipe(compressCompressedKey);
        Bukkit.removeRecipe(decompressCompressedKey);
        recipesRegistered = false;
    }

    /**
     * Registers a shapeless recipe that turns {@code count} copies of the
     * ingredient into one result item.
     *
     * @param key unique recipe key
     * @param ingredient the exact item consumed (its display name and lore
     *                   must match, so only real tokens compress)
     * @param result the item produced
     */
    private void registerCompress(final NamespacedKey key, final ItemStack ingredient,
                                  final ItemStack result) {
        final ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        final RecipeChoice.ExactChoice choice = new RecipeChoice.ExactChoice(ingredient);
        for (int i = 0; i < COMPRESS_RATIO; i++) {
            recipe.addIngredient(choice);
        }
        Bukkit.addRecipe(recipe);
    }

    /**
     * Registers a shapeless recipe that turns one ingredient item into
     * {@code count} copies of the result.
     *
     * @param key unique recipe key
     * @param ingredient the exact item consumed
     * @param result the item produced in a stack of {@value #COMPRESS_RATIO}
     */
    private void registerDecompress(final NamespacedKey key, final ItemStack ingredient,
                                    final ItemStack result) {
        final ItemStack stack = result.clone();
        stack.setAmount(COMPRESS_RATIO);
        final ShapelessRecipe recipe = new ShapelessRecipe(key, stack);
        recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));
        Bukkit.addRecipe(recipe);
    }

    /**
     * Builds a named, described item from the given material.
     *
     * @param material item material
     * @param name raw display name with {@code &} color codes
     * @param lore raw lore lines with {@code &} color codes
     * @return the finished item
     */
    private ItemStack build(final Material material, final String name, final List<String> lore) {
        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(KillTokenPlugin.color(name));
            meta.setLore(lore.stream().map(KillTokenPlugin::color).toList());
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
