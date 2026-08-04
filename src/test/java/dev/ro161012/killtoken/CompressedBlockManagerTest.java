package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Integration tests for the compressed Kill Token blocks and their
 * crafting recipes.
 */
final class CompressedBlockManagerTest {

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

    private List<String> lore(final ItemStack stack) {
        return ChatColor.stripColor(String.join("\n",
                stack.getItemMeta().getLore())).lines().toList();
    }

    private boolean recipeRegistered(final String key) {
        return server.getRecipe(new NamespacedKey(plugin, key)) != null;
    }

    private int recipeCount() {
        int count = 0;
        final java.util.Iterator<Recipe> it = server.recipeIterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    @Test
    @DisplayName("compressed block uses quartz and lists its value of 9 tokens")
    void compressedBlockHasValueInLore() {
        final ItemStack block = plugin.getCompressedBlockManager().createCompressedBlock();

        assertEquals(Material.QUARTZ_BLOCK, block.getType());
        assertTrue(block.getItemMeta().getDisplayName().contains("Compressed Kill Token Block"));
        final List<String> lines = lore(block);
        assertTrue(lines.stream().anyMatch(line -> line.contains("Value")
                && line.contains("9") && line.contains("Kill Tokens")));
    }

    @Test
    @DisplayName("compressed compressed block lists its value of 81 tokens")
    void compressedCompressedBlockHasValueInLore() {
        final ItemStack block = plugin.getCompressedBlockManager().createCompressedCompressedBlock();

        assertEquals(Material.SMOOTH_QUARTZ, block.getType());
        assertTrue(block.getItemMeta().getDisplayName()
                .contains("Compressed Compressed Kill Token Block"));
        final List<String> lines = lore(block);
        assertTrue(lines.stream().anyMatch(line -> line.contains("Value")
                && line.contains("81") && line.contains("Kill Tokens")));
        assertEquals(81, CompressedBlockManager.COMPRESSED_COMPRESSED_VALUE);
    }

    @Test
    @DisplayName("compression and decompression recipes are registered on enable")
    void registersAllRecipes() {
        assertTrue(recipeRegistered("compress"));
        assertTrue(recipeRegistered("decompress"));
        assertTrue(recipeRegistered("compress_compressed"));
        assertTrue(recipeRegistered("decompress_compressed"));
    }

    @Test
    @DisplayName("re-registering does not leave stale recipes behind")
    void reRegisteringIsIdempotent() {
        final int before = recipeCount();

        plugin.getCompressedBlockManager().registerRecipes();

        assertEquals(before, recipeCount(),
                "duplicate recipe registrations must be removed first");
    }

    @Test
    @DisplayName("disabled compressed blocks register no recipes")
    void disabledRegistersNothing() {
        plugin.getConfig().set("compressed-blocks.enabled", false);
        plugin.getCompressedBlockManager().registerRecipes();

        assertFalse(recipeRegistered("compress"));
        assertFalse(recipeRegistered("decompress_compressed"));
    }

    @Test
    @DisplayName("unregisterRecipes removes every registered recipe")
    void unregistersEverything() {
        plugin.getCompressedBlockManager().unregisterRecipes();

        assertFalse(recipeRegistered("compress"));
        assertFalse(recipeRegistered("decompress"));
        assertFalse(recipeRegistered("compress_compressed"));
        assertFalse(recipeRegistered("decompress_compressed"));
    }
}
