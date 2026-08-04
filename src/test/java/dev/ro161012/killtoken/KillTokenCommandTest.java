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
    @DisplayName("/killtoken give hands tokens to the executing player by default")
    void giveDefaultsToSelf() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give");

        assertEquals(1, tokensOnFloor());
        assertFalse(player.getInventory().contains(Material.NETHER_STAR),
                "tokens are dropped on the floor, never placed into inventories");
    }

    @Test
    @DisplayName("/killtoken give supports an explicit target and amount")
    void giveSupportsTargetAndAmount() {
        final PlayerMock sender = server.addPlayer();
        sender.setOp(true);
        final PlayerMock target = server.addPlayer();

        sender.performCommand("killtoken give " + target.getName() + " 3");

        assertEquals(3, tokensOnFloor());
        assertFalse(target.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    @DisplayName("/killtoken give works from the console with a target")
    void giveWorksFromConsole() {
        final PlayerMock target = server.addPlayer();

        server.dispatchCommand(server.getConsoleSender(), "killtoken give " + target.getName() + " 2");

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
        assertTrue(subs.containsAll(List.of("set", "give", "reload")));

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
    @DisplayName("/killtoken giveblock drops a compressed block to the executor by default")
    void giveBlockDefaultsToSelf() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken giveblock");

        final List<ItemStack> items = itemsOnFloor();
        assertEquals(1, items.size());
        assertEquals(Material.QUARTZ_BLOCK, items.get(0).getType());
        assertEquals(1, items.get(0).getAmount());
    }

    @Test
    @DisplayName("/killtoken giveblock supports a target, tier and amount")
    void giveBlockSupportsTargetTierAndAmount() {
        final PlayerMock sender = server.addPlayer();
        sender.setOp(true);
        final PlayerMock target = server.addPlayer();

        sender.performCommand("killtoken giveblock " + target.getName() + " 2 3");

        final List<ItemStack> items = itemsOnFloor();
        assertEquals(1, items.size());
        assertEquals(Material.SMOOTH_QUARTZ, items.get(0).getType());
        assertEquals(3, items.get(0).getAmount());
    }

    @Test
    @DisplayName("/killtoken giveblock rejects invalid tiers and amounts")
    void giveBlockRejectsInvalidArguments() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken giveblock " + player.getName() + " 3");
        player.performCommand("killtoken giveblock " + player.getName() + " 1 0");
        player.performCommand("killtoken giveblock " + player.getName()
                + " 1 " + (KillTokenCommand.MAX_BLOCK_GIVE_AMOUNT + 1));
        player.performCommand("killtoken giveblock " + player.getName() + " 1 bananas");

        assertEquals(0, itemsOnFloor().size());
    }

    @Test
    @DisplayName("players without permission cannot use giveblock")
    void giveBlockRequiresPermission() {
        final PlayerMock player = server.addPlayer();
        player.setOp(false);

        player.performCommand("killtoken giveblock");

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
