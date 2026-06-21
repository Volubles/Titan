package com.voluble.titanMC.donatortools.tool;

import com.voluble.titanMC.donatortools.MockBukkitDonatorToolsTestSupport;
import com.voluble.titanMC.donatortools.config.DonatorToolsConfiguration;
import com.voluble.titanMC.donatortools.item.DonatorToolRegistry;
import com.voluble.titanMC.donatortools.item.DonatorToolType;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplosiveToolListenerTest extends MockBukkitDonatorToolsTestSupport {

	@Test
	void naturallyBreaksOnlyAllowedSideAndTracksEachAdditionalBlockOnce() {
		var world = server.addSimpleWorld("explosive_listener");
		var player = server.addPlayer();
		var center = world.getBlockAt(10, 64, 10);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					center.getRelative(dx, dy, dz).setType(Material.STONE);
				}
			}
		}
		DonatorToolRegistry tools = new DonatorToolRegistry(MockBukkit.createMockPlugin());
		player.getInventory().setItemInMainHand(tools.create(DonatorToolType.EXPLOSIVE));
		List<org.bukkit.Location> tracked = new ArrayList<>();
		ExplosiveToolListener listener = new ExplosiveToolListener(
			tools,
			() -> new DonatorToolsConfiguration(true, Set.of(Material.STONE)),
			(actor, block) -> block.getX() <= center.getX(),
			tracked::add
		);
		BlockBreakEvent event = new BlockBreakEvent(center, player);

		listener.rememberTool(event);
		player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.AIR));
		listener.onBlockBreak(event);

		assertEquals(Material.AIR, center.getRelative(-1, 0, 0).getType());
		assertEquals(Material.STONE, center.getRelative(1, 0, 0).getType());
		assertEquals(Material.STONE, center.getType());
		assertEquals(17, tracked.size());
		assertEquals(17, tracked.stream().distinct().count());
	}
}
