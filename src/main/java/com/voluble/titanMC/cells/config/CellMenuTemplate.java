package com.voluble.titanMC.cells.config;

import java.util.Map;

public record CellMenuTemplate(int rows, String title, Map<String, CellItemTemplate> items) {
	public CellMenuTemplate {
		if (rows < 1 || rows > 6) throw new IllegalArgumentException("GUI rows must be between 1 and 6");
		items = Map.copyOf(items);
		for (CellItemTemplate item : items.values()) {
			if (item.slot() >= rows * 9) throw new IllegalArgumentException("GUI item slot is outside its menu");
		}
	}

	public CellItemTemplate item(String key) {
		CellItemTemplate item = items.get(key);
		if (item == null) throw new IllegalArgumentException("Missing GUI item: " + key);
		return item;
	}
}
