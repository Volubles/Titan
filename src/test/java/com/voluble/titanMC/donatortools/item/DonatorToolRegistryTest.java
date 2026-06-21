package com.voluble.titanMC.donatortools.item;

import com.voluble.titanMC.donatortools.MockBukkitDonatorToolsTestSupport;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonatorToolRegistryTest extends MockBukkitDonatorToolsTestSupport {

	@Test
	void persistentIdentityDoesNotDependOnDisplayName() {
		DonatorToolRegistry registry = new DonatorToolRegistry(MockBukkit.createMockPlugin());
		ItemStack item = registry.create(DonatorToolType.BOUNTIFUL);
		var meta = item.getItemMeta();
		meta.displayName(Component.text("Renamed by an anvil"));
		item.setItemMeta(meta);

		assertEquals(DonatorToolType.BOUNTIFUL, registry.identify(item).orElseThrow());
	}

	@Test
	void ordinaryAndLegacyNameOnlyPickaxesAreNotDonatorTools() {
		DonatorToolRegistry registry = new DonatorToolRegistry(MockBukkit.createMockPlugin());
		ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
		var meta = item.getItemMeta();
		meta.displayName(Component.text("Bountiful Pickaxe"));
		item.setItemMeta(meta);

		assertTrue(registry.identify(item).isEmpty());
		assertEquals(DonatorToolType.COMPRESSED, registry.find("block").orElseThrow());
	}
}
