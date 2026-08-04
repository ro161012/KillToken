package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for the compressed block placement protection.
 */
final class CompressedBlockListenerTest {

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

    private BlockPlaceEvent placeEvent(final PlayerMock player, final ItemStack held) {
        final Block block = server.getWorlds().get(0).getBlockAt(0, 64, 0);
        final BlockState replaced = block.getState();
        final Block against = server.getWorlds().get(0).getBlockAt(0, 63, 0);
        return new BlockPlaceEvent(block, replaced, against, held, player, true);
    }

    @Test
    @DisplayName("placing a compressed Kill Token block is cancelled")
    void placementOfCompressedBlockIsCancelled() {
        final PlayerMock player = server.addPlayer();
        final ItemStack held = plugin.getCompressedBlockManager().createCompressedBlock();

        final BlockPlaceEvent event = placeEvent(player, held);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("placing a normal quartz block is allowed")
    void placementOfNormalQuartzIsAllowed() {
        final PlayerMock player = server.addPlayer();

        final BlockPlaceEvent event = placeEvent(player, new ItemStack(Material.QUARTZ_BLOCK));
        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
    }
}
