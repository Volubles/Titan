package com.voluble.titanMC.mines.breaking;

import org.bukkit.Material;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public sealed interface MineBreakProfile permits MineBreakProfile.Unrestricted, MineBreakProfile.AllowList {
	boolean allows(Material material);

	record Unrestricted() implements MineBreakProfile {
		@Override
		public boolean allows(Material material) {
			return Objects.requireNonNull(material, "material").isBlock();
		}
	}

	record AllowList(Set<Material> materials) implements MineBreakProfile {
		public AllowList {
			Objects.requireNonNull(materials, "materials");
			LinkedHashSet<Material> validated = new LinkedHashSet<>();
			for (Material material : materials) {
				Objects.requireNonNull(material, "materials must not contain null");
				if (!material.isBlock() || material.isAir()) {
					throw new IllegalArgumentException("Diggable materials must be non-air blocks: " + material);
				}
				validated.add(material);
			}
			materials = Set.copyOf(validated);
		}

		@Override
		public boolean allows(Material material) {
			return materials.contains(Objects.requireNonNull(material, "material"));
		}

		public AllowList with(Material material) {
			LinkedHashSet<Material> updated = new LinkedHashSet<>(materials);
			updated.add(material);
			return new AllowList(updated);
		}

		public AllowList without(Material material) {
			LinkedHashSet<Material> updated = new LinkedHashSet<>(materials);
			updated.remove(material);
			return new AllowList(updated);
		}
	}
}
