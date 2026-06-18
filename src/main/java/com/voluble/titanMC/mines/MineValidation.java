package com.voluble.titanMC.mines;

import com.voluble.titanMC.util.RegionUtils;

import java.util.regex.Pattern;

public final class MineValidation {

	public static final long MAX_VOLUME = 20_000_000L;
	private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

	private MineValidation() {}

	public static String validateName(String name) {
		if (name == null || !VALID_NAME.matcher(name).matches()) {
			return "Names must be 1-32 characters and only use letters, numbers, _ or -.";
		}
		return null;
	}

	public static String validateCuboid(RegionUtils.Cuboid cuboid) {
		long sizeX = (long) cuboid.maxX - cuboid.minX + 1L;
		long sizeY = (long) cuboid.maxY - cuboid.minY + 1L;
		long sizeZ = (long) cuboid.maxZ - cuboid.minZ + 1L;
		long volume;
		try {
			volume = Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);
		} catch (ArithmeticException exception) {
			return "The selection is too large.";
		}
		if (volume > MAX_VOLUME) {
			return "The selection is too large (maximum " + MAX_VOLUME + " blocks).";
		}
		return null;
	}
}
