package com.voluble.titanMC.donatortools.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class DonatorToolRegistry {

	private final NamespacedKey toolKey;

	public DonatorToolRegistry(Plugin plugin) {
		this.toolKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), "donator_tool");
	}

	public ItemStack create(DonatorToolType type) {
		Objects.requireNonNull(type, "type");
		ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(type.displayName(), type.color()));
		meta.lore(List.of(Component.text(type.description(), NamedTextColor.YELLOW)));
		meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, type.id());
		item.setItemMeta(meta);
		return item;
	}

	public Optional<DonatorToolType> identify(ItemStack item) {
		if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
		String id = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
		if (id == null) return Optional.empty();
		return Arrays.stream(DonatorToolType.values())
			.filter(type -> type.id().equals(id))
			.findFirst();
	}

	public Optional<DonatorToolType> find(String input) {
		if (input == null) return Optional.empty();
		String normalized = input.toLowerCase(Locale.ROOT);
		if (normalized.equals("compressed") || normalized.equals("compressedpickaxe")) {
			return Optional.of(DonatorToolType.COMPRESSED);
		}
		return Arrays.stream(DonatorToolType.values())
			.filter(type -> type.id().equals(normalized)
				|| (type.id() + "pickaxe").equals(normalized))
			.findFirst();
	}

	public List<String> ids() {
		return Arrays.stream(DonatorToolType.values()).map(DonatorToolType::id).toList();
	}
}
