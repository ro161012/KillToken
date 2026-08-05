package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Integration tests for {@code /killtoken}.
 */
final class KillTokenCommandTest {

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

    private int tokensOnFloor() {
        return server.getWorlds().get(0).getEntities().stream()
                .filter(entity -> entity.getType() == EntityType.ITEM)
                .map(Item.class::cast)
                .filter(item -> item.getItemStack().getType() == Material.NETHER_STAR)
                .mapToInt(item -> item.getItemStack().getAmount())
                .sum();
    }

    private int tokensInInventory(final PlayerMock player) {
        return player.getInventory().all(Material.NETHER_STAR).values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    @Test
    @DisplayName("/killtoken set uses the main-hand item as the new currency")
    void setUsesMainHandItem() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.EMERALD));

        player.performCommand("killtoken set");

        assertEquals(Material.EMERALD, plugin.getCurrencyItem().getType());
    }

    @Test
    @DisplayName("/killtoken set rejects an empty main hand")
    void setRejectsEmptyHand() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken set");

        assertEquals(Material.NETHER_STAR, plugin.getCurrencyItem().getType(),
                "currency must remain unchanged");
    }

    @Test
    @DisplayName("/killtoken give places tokens into the executing player's inventory")
    void giveDefaultsToSelf() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give");

        assertEquals(1, tokensInInventory(player));
        assertEquals(0, tokensOnFloor());
    }

    @Test
    @DisplayName("/killtoken give supports an explicit target and amount")
    void giveSupportsTargetAndAmount() {
        final PlayerMock sender = server.addPlayer();
        sender.setOp(true);
        final PlayerMock target = server.addPlayer();

        sender.performCommand("killtoken give " + target.getName() + " 3");

        assertEquals(3, tokensInInventory(target));
        assertEquals(0, tokensOnFloor());
    }

    @Test
    @DisplayName("/killtoken give works from the console with a target")
    void giveWorksFromConsole() {
        final PlayerMock target = server.addPlayer();

        server.dispatchCommand(server.getConsoleSender(), "killtoken give " + target.getName() + " 2");

        assertEquals(2, tokensInInventory(target));
    }

    @Test
    @DisplayName("/killtoken give merges with existing stacks in the inventory")
    void giveMergesWithExistingStacks() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give " + player.getName() + " 10");
        player.performCommand("killtoken give " + player.getName() + " 10");

        assertEquals(20, tokensInInventory(player), "stacks must merge, not overwrite");
    }

    @Test
    @DisplayName("/killtoken give drops overflow at the feet instead of losing it")
    void giveDropsOverflowOnFloor() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);
        // MockBukkit scans all 41 slots (storage + armor + offhand) when
        // adding items, so fill every slot to force an overflow.
        final ItemStack filler = new ItemStack(Material.DIRT, 64);
        for (int slot = 0; slot < 41; slot++) {
            player.getInventory().setItem(slot, filler.clone());
        }

        player.performCommand("killtoken give " + player.getName() + " 2");

        assertEquals(0, tokensInInventory(player));
        assertEquals(2, tokensOnFloor());
    }

    @Test
    @DisplayName("/killtoken give rejects an offline target")
    void giveRejectsOfflineTarget() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give NoSuchPlayer 1");

        assertEquals(0, tokensOnFloor());
    }

    @Test
    @DisplayName("/killtoken give rejects invalid amounts")
    void giveRejectsInvalidAmounts() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give " + player.getName() + " 0");
        player.performCommand("killtoken give " + player.getName() + " " + (KillTokenCommand.MAX_GIVE_AMOUNT + 1));
        player.performCommand("killtoken give " + player.getName() + " bananas");

        assertEquals(0, tokensOnFloor());
    }

    @Test
    @DisplayName("players without permission cannot use give")
    void giveRequiresPermission() {
        final PlayerMock player = server.addPlayer();
        player.setOp(false);

        player.performCommand("killtoken give");

        assertEquals(0, tokensOnFloor());
    }

    @Test
    @DisplayName("tab completion offers subcommands and player names")
    void tabCompletionWorks() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        final List<String> subs = plugin.getCommand("killtoken")
                .tabComplete(player, "killtoken", new String[]{""});
        assertTrue(subs.containsAll(List.of("set", "give", "giveblock", "reload", "test")));

        final List<String> giveTargets = plugin.getCommand("killtoken")
                .tabComplete(player, "killtoken", new String[]{"give", ""});
        assertTrue(giveTargets.contains(player.getName()));
    }

    private List<ItemStack> itemsOnFloor() {
        return server.getWorlds().get(0).getEntities().stream()
                .filter(entity -> entity.getType() == EntityType.ITEM)
                .map(Item.class::cast)
                .map(Item::getItemStack)
                .toList();
    }

    @Test
    @DisplayName("/killtoken test previews the reward milestone without changing a real streak")
    void testPreviewsKillstreakMilestone() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken test");

        assertEquals(2, tokensInInventory(player));
        assertEquals(0, plugin.getKillstreakTracker().get(player.getUniqueId()));
        assertEquals(0, tokensOnFloor());
    }

    @Test
    @DisplayName("/killtoken giveblock places a compressed block into the executor's inventory")
    void giveBlockDefaultsToSelf() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken giveblock");

        final ItemStack block = player.getInventory().getItem(0);
        assertEquals(Material.QUARTZ_BLOCK, block.getType());
        assertEquals(1, block.getAmount());
        assertEquals(0, itemsOnFloor().size());
    }

    @Test
    @DisplayName("/killtoken giveblock supports a target and amount")
    void giveBlockSupportsTargetAndAmount() {
        final PlayerMock sender = server.addPlayer();
        sender.setOp(true);
        final PlayerMock target = server.addPlayer();

        sender.performCommand("killtoken giveblock " + target.getName() + " 3");

        final ItemStack block = target.getInventory().getItem(0);
        assertEquals(Material.QUARTZ_BLOCK, block.getType());
        assertEquals(3, block.getAmount());
        assertEquals(0, itemsOnFloor().size());
    }

    @Test
    @DisplayName("/killtoken giveblock rejects invalid amounts")
    void giveBlockRejectsInvalidArguments() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken giveblock " + player.getName() + " 0");
        player.performCommand("killtoken giveblock " + player.getName()
                + " " + (KillTokenCommand.MAX_BLOCK_GIVE_AMOUNT + 1));
        player.performCommand("killtoken giveblock " + player.getName() + " bananas");

        assertFalse(player.getInventory().contains(Material.QUARTZ_BLOCK));
        assertEquals(0, itemsOnFloor().size());
    }

    @Test
    @DisplayName("players without permission cannot use giveblock")
    void giveBlockRequiresPermission() {
        final PlayerMock player = server.addPlayer();
        player.setOp(false);

        player.performCommand("killtoken giveblock");

        assertFalse(player.getInventory().contains(Material.QUARTZ_BLOCK));
        assertEquals(0, itemsOnFloor().size());
    }

    @Test
    @DisplayName("unknown subcommands fall back to usage without side effects")
    void unknownSubcommandShowsUsage() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken explode");

        assertEquals(Material.NETHER_STAR, plugin.getCurrencyItem().getType());
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }
}
