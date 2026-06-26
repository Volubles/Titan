package com.voluble.titanMC.milestones.ui;

import com.voluble.titanMC.milestones.config.MilestoneCatalog;
import com.voluble.titanMC.milestones.model.MilestoneCategory;
import com.voluble.titanMC.milestones.model.MilestoneProgress;
import com.voluble.titanMC.milestones.model.MilestoneTier;
import com.voluble.titanMC.milestones.model.MilestoneTrack;
import com.voluble.titanMC.milestones.service.MilestoneService;
import com.voluble.titanMC.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class MilestoneItemFactory {
	private final MilestoneService milestones;
	private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);

	MilestoneItemFactory(MilestoneService milestones) {
		this.milestones = Objects.requireNonNull(milestones, "milestones");
	}

	ItemStack category(Player player, MilestoneCategory category, MilestoneCatalog catalog) {
		ItemStack item = new ItemStack(category.enabled() ? category.icon() : Material.GRAY_DYE);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;
		meta.displayName(ChatUtils.formatItem((category.enabled() ? "<#30bbf1><bold>" : "<gray><bold>") + category.name()));
		List<Component> lore = new ArrayList<>();
		if (!category.enabled()) {
			lore.add(ChatUtils.formatItem("<gray>Coming soon"));
		} else {
			CategorySummary summary = summary(player, catalog.tracks(category.id()));
			lore.add(ChatUtils.formatItem("<gray>Completed: <white>" + summary.completed + " / " + summary.total));
			if (summary.current != null) {
				lore.add(ChatUtils.formatItem("<gray>Current: <#f7d774>" + summary.current));
			}
			lore.add(Component.empty());
			lore.add(ChatUtils.formatItem("<green>Click to view"));
		}
		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	ItemStack track(Player player, MilestoneTrack track) {
		ItemStack item = new ItemStack(track.icon());
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;
		MilestoneProgress progress = milestones.progress(player.getUniqueId(), track.metric(), track.subject());
		meta.displayName(ChatUtils.formatItem("<#30bbf1><bold>" + track.name()));
		List<Component> lore = new ArrayList<>();
		lore.add(ChatUtils.formatItem("<gray>Progress: <white>" + numbers.format(progress.amount())));
		lore.add(Component.empty());
		boolean foundCurrent = false;
		for (MilestoneTier tier : track.tiers()) {
			boolean completed = milestones.completed(player.getUniqueId(), tier.id()) || progress.amount() >= tier.target();
			boolean current = !completed && !foundCurrent;
			if (current) foundCurrent = true;
			String prefix = completed ? "DONE" : current ? "NEXT" : "LOCKED";
			String color = completed ? "<green>" : current ? "<#f7d774>" : "<gray>";
			String value = completed
				? numbers.format(tier.target()) + " / " + numbers.format(tier.target())
				: numbers.format(Math.min(progress.amount(), tier.target())) + " / " + numbers.format(tier.target());
			lore.add(ChatUtils.formatItem(color + prefix + " <white>" + tier.name() + " <dark_gray>- <gray>" + value));
		}
		item.setItemMeta(meta);
		return item;
	}

	ItemStack back() {
		return simple(Material.ARROW, "<yellow>Back");
	}

	private ItemStack simple(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return item;
		meta.displayName(ChatUtils.formatItem(name));
		item.setItemMeta(meta);
		return item;
	}

	private CategorySummary summary(Player player, List<MilestoneTrack> tracks) {
		int completed = 0;
		int total = 0;
		String current = null;
		for (MilestoneTrack track : tracks) {
			MilestoneProgress progress = milestones.progress(player.getUniqueId(), track.metric(), track.subject());
			for (MilestoneTier tier : track.tiers()) {
				total++;
				if (milestones.completed(player.getUniqueId(), tier.id()) || progress.amount() >= tier.target()) {
					completed++;
				} else if (current == null) {
					current = tier.name() + " (" + numbers.format(Math.min(progress.amount(), tier.target()))
						+ " / " + numbers.format(tier.target()) + ")";
				}
			}
		}
		return new CategorySummary(completed, total, current);
	}

	private record CategorySummary(int completed, int total, String current) {
	}
}
