package dev.ro161012.killtoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
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

    private static int countTokens(final PlayerMock player) {
        int count = 0;
        for (final ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == Material.NETHER_STAR) {
                count += stack.getAmount();
            }
        }
        return count;
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

        assertEquals(1, countTokens(player));
    }

    @Test
    @DisplayName("/killtoken give supports an explicit target and amount")
    void giveSupportsTargetAndAmount() {
        final PlayerMock sender = server.addPlayer();
        sender.setOp(true);
        final PlayerMock target = server.addPlayer();

        sender.performCommand("killtoken give " + target.getName() + " 3");

        assertEquals(3, countTokens(target));
        assertEquals(0, countTokens(sender));
    }

    @Test
    @DisplayName("/killtoken give works from the console with a target")
    void giveWorksFromConsole() {
        final PlayerMock target = server.addPlayer();

        server.dispatchCommand(server.getConsoleSender(), "killtoken give " + target.getName() + " 2");

        assertEquals(2, countTokens(target));
    }

    @Test
    @DisplayName("/killtoken give rejects an offline target")
    void giveRejectsOfflineTarget() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give NoSuchPlayer 1");

        assertEquals(0, countTokens(player));
    }

    @Test
    @DisplayName("/killtoken give rejects invalid amounts")
    void giveRejectsInvalidAmounts() {
        final PlayerMock player = server.addPlayer();
        player.setOp(true);

        player.performCommand("killtoken give " + player.getName() + " 0");
        player.performCommand("killtoken give " + player.getName() + " " + (KillTokenCommand.MAX_GIVE_AMOUNT + 1));
        player.performCommand("killtoken give " + player.getName() + " bananas");

        assertEquals(0, countTokens(player));
    }

    @Test
    @DisplayName("players without permission cannot use give")
    void giveRequiresPermission() {
        final PlayerMock player = server.addPlayer();
        player.setOp(false);

        player.performCommand("killtoken give");

        assertEquals(0, countTokens(player));
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
