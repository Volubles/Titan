package com.voluble.titanMC.outfits.skin;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutfitSkinComposerTest {
	@Test
	void playerHeadFullyReplacesTemplateHeadLayer() {
		BufferedImage original = skin(new Color(30, 120, 220, 255));
		BufferedImage template = skin(new Color(220, 40, 40, 255));
		template.setRGB(40, 8, new Color(250, 200, 20, 255).getRGB());
		original.setRGB(40, 8, new Color(0, 0, 0, 0).getRGB());

		BufferedImage composed = new OutfitSkinComposer().compose(original, template);

		assertEquals(0, new Color(composed.getRGB(40, 8), true).getAlpha());
	}

	@Test
	void playerHeadBaseStillReplacesTemplateHeadBase() {
		BufferedImage original = skin(new Color(30, 120, 220, 255));
		BufferedImage template = skin(new Color(220, 40, 40, 255));

		BufferedImage composed = new OutfitSkinComposer().compose(original, template);

		assertEquals(original.getRGB(8, 8), composed.getRGB(8, 8));
	}

	private static BufferedImage skin(Color fill) {
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				image.setRGB(x, y, fill.getRGB());
			}
		}
		return image;
	}
}
