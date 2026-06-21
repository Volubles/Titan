package com.voluble.titanMC.donatortools.tool;

import com.voluble.titanMC.donatortools.MockBukkitDonatorToolsTestSupport;
import com.voluble.titanMC.donatortools.config.DonatorToolsConfiguration;
import com.voluble.titanMC.donatortools.drop.BlockDropService;
import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SingleBlockToolListenerTest extends MockBukkitDonatorToolsTestSupport {

	private Plugin plugin;
	private DonatorToolRegistry registry;

	@BeforeEach
	void createRegistry() {
		plugin = MockBukkit.createMockPlugin();
		registry = new DonatorToolRegistry(plugin);
	}

	@Test
	void smeltingPreservesTheVanillaFortuneAmountAndItemPhysics() {
		var fixture = fixture(Material.IRON_ORE, Material.RAW_IRON, 5);
		fixture.player().getInventory().setItemInMainHand(registry.create(DonatorToolType.SMELTING));
		Vector velocity = new Vector(0.1, 0.2, -0.1);
		fixture.item().setVelocity(velocity);

		SingleBlockToolListener listener = listener((state, tool, player) -> List.of());
		listener.rememberTool(fixture.breakEvent());
		fixture.player().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
		listener.onBlockDrops(fixture.event());

		assertEquals(new ItemStack(Material.IRON_INGOT, 5), fixture.item().getItemStack());
		assertEquals(velocity, fixture.item().getVelocity());
	}

	@Test
	void compressedReplacesVanillaLootWithOneBlockForm() {
		var fixture = fixture(Material.DIAMOND_ORE, Material.DIAMOND, 4);
		fixture.player().getInventory().setItemInMainHand(registry.create(DonatorToolType.COMPRESSED));

		SingleBlockToolListener listener = listener((state, tool, player) -> List.of());
		listener.rememberTool(fixture.breakEvent());
		listener.onBlockDrops(fixture.event());

		assertEquals(new ItemStack(Material.DIAMOND_BLOCK), fixture.item().getItemStack());
	}

	@Test
	void bountifulUsesVanillaLootForTheHighestRankedNearbyOre() {
		var fixture = fixture(Material.STONE, Material.COBBLESTONE, 1);
		fixture.block().getRelative(1, 0, 0).setType(Material.IRON_ORE);
		fixture.block().getRelative(-1, 0, 0).setType(Material.DIAMOND_ORE);
		fixture.player().getInventory().setItemInMainHand(registry.create(DonatorToolType.BOUNTIFUL));

		SingleBlockToolListener listener = listener((state, tool, player) -> {
			assertEquals(Material.DIAMOND_ORE, state.getType());
			return List.of(new ItemStack(Material.DIAMOND, 3));
		});
		listener.rememberTool(fixture.breakEvent());
		listener.onBlockDrops(fixture.event());

		assertEquals(new ItemStack(Material.DIAMOND, 3), fixture.item().getItemStack());
	}

	private SingleBlockToolListener listener(
		com.voluble.titanMC.donatortools.drop.VanillaLoot loot
	) {
		return new SingleBlockToolListener(
			registry,
			() -> new DonatorToolsConfiguration(true, Set.of()),
			new BlockDropService(),
			loot
		);
	}

	private Fixture fixture(Material blockType, Material vanillaDrop, int amount) {
		var world = server.addSimpleWorld("single_" + blockType.name().toLowerCase());
		var player = server.addPlayer();
		Block block = world.getBlockAt(4, 64, 4);
		block.setType(blockType);
		BlockState state = block.getState();
		Item item = world.dropItem(
			new Location(world, 4.4, 64.3, 4.6),
			new ItemStack(vanillaDrop, amount)
		);
		BlockDropItemEvent event = new BlockDropItemEvent(
			block,
			state,
			player,
			new ArrayList<>(List.of(item))
		);
		return new Fixture(player, block, item, new BlockBreakEvent(block, player), event);
	}

	private record Fixture(
		org.mockbukkit.mockbukkit.entity.PlayerMock player,
		Block block,
		Item item,
		BlockBreakEvent breakEvent,
		BlockDropItemEvent event
	) {}
}
