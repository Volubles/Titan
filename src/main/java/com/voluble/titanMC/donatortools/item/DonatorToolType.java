package com.voluble.titanMC.donatortools.item;

import net.kyori.adventure.text.format.NamedTextColor;

public enum DonatorToolType {
	SMELTING("smelting", "Smelting Pickaxe", "Automatically smelts mined ores.", NamedTextColor.GOLD),
	EXPLOSIVE("explosive", "Explosive Pickaxe", "Breaks a protected 3x3x3 area.", NamedTextColor.RED),
	BOUNTIFUL("bountiful", "Bountiful Pickaxe", "Drops the best nearby ore.", NamedTextColor.AQUA),
	COMPRESSED("block", "Block Pickaxe", "Converts mined ores into block form.", NamedTextColor.LIGHT_PURPLE);

	private final String id;
	private final String displayName;
	private final String description;
	private final NamedTextColor color;

	DonatorToolType(String id, String displayName, String description, NamedTextColor color) {
		this.id = id;
		this.displayName = displayName;
		this.description = description;
		this.color = color;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}

	public NamedTextColor color() {
		return color;
	}
}
