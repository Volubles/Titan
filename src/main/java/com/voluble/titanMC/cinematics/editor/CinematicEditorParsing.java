package com.voluble.titanMC.cinematics.editor;

final class CinematicEditorParsing {
	private CinematicEditorParsing() {
	}

	static int nonNegativeInt(String value) {
		int parsed = Integer.parseInt(value.trim());
		if (parsed < 0) throw new NumberFormatException("value must not be negative");
		return parsed;
	}

	static int positiveInt(String value) {
		int parsed = Integer.parseInt(value.trim());
		if (parsed <= 0) throw new NumberFormatException("value must be positive");
		return parsed;
	}

	static double decimal(String value) {
		return Double.parseDouble(value.trim());
	}

	static float nonNegativeFloat(String value) {
		float parsed = Float.parseFloat(value.trim());
		if (parsed < 0.0f) throw new NumberFormatException("value must not be negative");
		return parsed;
	}

	static double[] vector3(String value) {
		String[] parts = value.trim().split("\\s+");
		if (parts.length != 3) throw new NumberFormatException("expected three numbers");
		return new double[] {
			Double.parseDouble(parts[0]),
			Double.parseDouble(parts[1]),
			Double.parseDouble(parts[2])
		};
	}
}
