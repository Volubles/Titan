package com.voluble.titanMC.mines;

import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Uniform weighted palette of Materials for mines.
 */
public final class WeightedPalette {

	private final LinkedHashMap<Material, Integer> materialToWeight;
	private int totalWeight;

	public WeightedPalette() {
		this.materialToWeight = new LinkedHashMap<>();
		this.totalWeight = 0;
	}

	public WeightedPalette(Map<Material, Integer> materialWeights) {
		this();
		if (materialWeights != null) {
			for (Map.Entry<Material, Integer> e : materialWeights.entrySet()) {
				addOrUpdate(e.getKey(), e.getValue());
			}
		}
	}

	public void addOrUpdate(Material material, int weight) {
		if (material == null) return;
		if (weight <= 0) {
			remove(material);
			return;
		}
		Integer prev = materialToWeight.put(material, weight);
		if (prev != null) totalWeight -= prev;
		totalWeight += weight;
	}

	public void remove(Material material) {
		Integer prev = materialToWeight.remove(material);
		if (prev != null) totalWeight -= prev;
	}

	public boolean isEmpty() {
		return materialToWeight.isEmpty() || totalWeight <= 0;
	}

	public int getTotalWeight() { return Math.max(0, totalWeight); }

	public Map<Material, Integer> getEntriesView() {
		return Collections.unmodifiableMap(materialToWeight);
	}

	public Material pickRandom(Random random) {
		if (isEmpty()) return Material.AIR;
		int bound = getTotalWeight();
		int r = random.nextInt(bound);
		int cumulative = 0;
		for (Map.Entry<Material, Integer> e : materialToWeight.entrySet()) {
			cumulative += e.getValue();
			if (r < cumulative) return e.getKey();
		}
		// Fallback (should not happen)
		return materialToWeight.keySet().iterator().next();
	}

	public Material pickRandomThreadLocal() {
		return pickRandom(ThreadLocalRandom.current());
	}

	public Map<String, Integer> toConfigMap() {
		LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
		for (Map.Entry<Material, Integer> e : materialToWeight.entrySet()) {
			out.put(e.getKey().name(), e.getValue());
		}
		return out;
	}

	public static WeightedPalette fromConfigMap(Map<String, Object> map) {
		WeightedPalette p = new WeightedPalette();
		if (map == null) return p;
		for (Map.Entry<String, Object> e : map.entrySet()) {
			Material m;
			try {
				m = Material.valueOf(e.getKey());
			} catch (IllegalArgumentException ex) {
				continue;
			}
			int weight = 0;
			Object v = e.getValue();
			if (v instanceof Number) weight = ((Number) v).intValue();
			else if (v instanceof String) {
				try { weight = Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
			}
			if (weight > 0) p.addOrUpdate(m, weight);
		}
		return p;
	}
}


