package com.voluble.titanMC.donatortools.drop;

import com.voluble.titanMC.donatortools.MockBukkitDonatorToolsTestSupport;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BlockDropServiceTest extends MockBukkitDonatorToolsTestSupport {

	@Test
	void transformsTheExistingVanillaItemEntityWithoutChangingItsPhysics() {
		var world = server.addSimpleWorld("natural_drop");
		var player = server.addPlayer();
		Block block = world.getBlockAt(1, 64, 1);
		block.setType(Material.IRON_ORE);
		var state = block.getState();
		block.setType(Material.AIR);
		Item item = world.dropItem(
			new Location(world, 1.37, 64.22, 1.61),
			new ItemStack(Material.RAW_IRON, 4)
		);
		Vector velocity = new Vector(0.08, 0.21, -0.04);
		item.setVelocity(velocity);
		item.setPickupDelay(12);
		List<Item> items = new ArrayList<>(List.of(item));
		BlockDropItemEvent event = new BlockDropItemEvent(block, state, player, items);

		new BlockDropService().transform(
			event,
			vanilla -> List.of(new ItemStack(Material.IRON_INGOT, vanilla.getFirst().getAmount()))
		);

		assertSame(item, event.getItems().getFirst());
		assertEquals(new ItemStack(Material.IRON_INGOT, 4), item.getItemStack());
		assertEquals(velocity, item.getVelocity());
		assertEquals(12, item.getPickupDelay());
	}

	@Test
	void refusesToArtificiallySpawnDropsWhenVanillaCreatedNoEntity() {
		var world = server.addSimpleWorld("empty_drop");
		var player = server.addPlayer();
		Block block = world.getBlockAt(1, 64, 1);
		block.setType(Material.GLASS);
		var state = block.getState();
		block.setType(Material.AIR);
		BlockDropItemEvent event = new BlockDropItemEvent(
			block,
			state,
			player,
			new ArrayList<>()
		);

		boolean transformed = new BlockDropService().transform(
			event,
			ignored -> List.of(new ItemStack(Material.DIAMOND))
		);

		assertFalse(transformed);
		assertEquals(0, event.getItems().size());
	}
}
