package com.voluble.titanMC.outfits.skin;

import com.voluble.titanMC.outfits.model.SkinModel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSkinSourceTest {
	@Test
	void readsSlimSkinFromTextureProperty() {
		String texture = """
			{
			  "textures": {
			    "SKIN": {
			      "url": "http://textures.minecraft.net/texture/original",
			      "metadata": { "model": "slim" }
			    }
			  }
			}
			""";

		PlayerSkin skin = new PlayerSkinSource().skin(property(texture)).orElseThrow();

		assertEquals("http://textures.minecraft.net/texture/original", skin.url().toString());
		assertEquals(SkinModel.SLIM, skin.model());
	}

	@Test
	void defaultsTexturePropertyToClassicSkinModel() {
		String texture = """
			{
			  "textures": {
			    "SKIN": {
			      "url": "http://textures.minecraft.net/texture/original"
			    }
			  }
			}
			""";

		PlayerSkin skin = new PlayerSkinSource().skin(property(texture)).orElseThrow();

		assertEquals(SkinModel.CLASSIC, skin.model());
	}

	@Test
	void ignoresInvalidTextureProperty() {
		assertTrue(new PlayerSkinSource().skin(new SkinPropertyData("not-base64", "signature")).isEmpty());
	}

	private static SkinPropertyData property(String textureJson) {
		return new SkinPropertyData(
			Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8)),
			"signature"
		);
	}
}
