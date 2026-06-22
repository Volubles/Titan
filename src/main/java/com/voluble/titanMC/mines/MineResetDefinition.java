package com.voluble.titanMC.mines;

import java.util.Locale;
import java.util.Objects;

public sealed interface MineResetDefinition permits MineResetDefinition.Palette, MineResetDefinition.Template {
	record Palette() implements MineResetDefinition {
	}

	record Template(String templateId) implements MineResetDefinition {
		public Template {
			templateId = normalizeTemplateId(templateId);
		}
	}

	public static String normalizeTemplateId(String value) {
		String normalized = Objects.requireNonNull(value, "templateId").toLowerCase(Locale.ROOT);
		if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
			throw new IllegalArgumentException("Template ids may only contain a-z, 0-9, _ and -");
		}
		return normalized;
	}
}
