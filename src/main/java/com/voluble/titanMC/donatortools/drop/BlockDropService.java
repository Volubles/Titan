package com.voluble.titanMC.donatortools.drop;

import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BlockDropService {

	public boolean transform(BlockDropItemEvent event, DropTransformation transformation) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(transformation, "transformation");
		List<Item> entities = event.getItems();
		List<ItemStack> vanilla = entities.stream()
			.map(Item::getItemStack)
			.map(ItemStack::clone)
			.toList();
		List<ItemStack> transformed = sanitize(transformation.transform(vanilla));
		if (transformed.isEmpty()) {
			entities.clear();
			return true;
		}
		if (entities.isEmpty()) {
			return false;
		}
		if (transformed.size() > entities.size()) {
			return false;
		}
		for (int index = 0; index < transformed.size(); index++) {
			ItemStack stack = transformed.get(index);
			if (index < entities.size()) {
				entities.get(index).setItemStack(stack);
			}
		}
		while (entities.size() > transformed.size()) {
			entities.removeLast();
		}
		return true;
	}

	private static List<ItemStack> sanitize(List<ItemStack> drops) {
		Objects.requireNonNull(drops, "transformation returned null");
		List<ItemStack> sanitized = new ArrayList<>();
		for (ItemStack drop : drops) {
			if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) continue;
			sanitized.add(drop.clone());
		}
		return List.copyOf(sanitized);
	}
}
