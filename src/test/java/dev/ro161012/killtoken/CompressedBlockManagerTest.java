package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Integration tests for the compressed Kill Token block item.
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

    @Test
    @DisplayName("compressed block uses quartz and lists its value of 64 tokens")
    void compressedBlockHasValueInLore() {
        final ItemStack block = plugin.getCompressedBlockManager().createCompressedBlock();

        assertEquals(Material.QUARTZ_BLOCK, block.getType());
        assertEquals("Compressed Kill Token Block",
                ChatColor.stripColor(block.getItemMeta().getDisplayName()));
        assertEquals(64, CompressedBlockManager.COMPRESS_RATIO);

        final List<String> lines = lore(block);
        assertTrue(lines.stream().anyMatch(line -> line.contains("Value")
                && line.contains("64") && line.contains("Kill Tokens")));
    }

    @Test
    @DisplayName("compressed block lore is plain, like the Kill Token lore")
    void compressedBlockLoreIsPlain() {
        final ItemStack block = plugin.getCompressedBlockManager().createCompressedBlock();

        final List<String> lines = lore(block);
        assertEquals(2, lines.size());
        assertEquals("A compact block of Kill Tokens.", lines.get(0));
        assertTrue(lines.get(1).startsWith("Value: 64 Kill Tokens"));
    }

    @Test
    @DisplayName("compressed block has a hidden enchantment for the enchanted glint")
    void compressedBlockIsEnchanted() {
        final ItemStack block = plugin.getCompressedBlockManager().createCompressedBlock();
        final org.bukkit.inventory.meta.ItemMeta meta = block.getItemMeta();

        assertFalse(meta.getEnchants().isEmpty(), "block must carry an enchantment for the glint");
        assertTrue(meta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS),
                "enchantment must be hidden so the tooltip stays plain");
    }
}
