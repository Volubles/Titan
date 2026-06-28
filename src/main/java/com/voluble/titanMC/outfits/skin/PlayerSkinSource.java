package com.voluble.titanMC.outfits.skin;

import com.voluble.titanMC.outfits.model.SkinModel;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerSkinSource {
	private static final Pattern SKIN_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern SKIN_MODEL = Pattern.compile("\"model\"\\s*:\\s*\"slim\"", Pattern.CASE_INSENSITIVE);

	public Optional<PlayerSkin> skin(SkinPropertyData property) {
		Objects.requireNonNull(property, "property");
		String textureJson;
		try {
			textureJson = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
		int skinIndex = textureJson.indexOf("\"SKIN\"");
		if (skinIndex < 0) return Optional.empty();
		Matcher url = SKIN_URL.matcher(textureJson);
		if (!url.find(skinIndex)) return Optional.empty();
		SkinModel model = SKIN_MODEL.matcher(textureJson.substring(skinIndex)).find() ? SkinModel.SLIM : SkinModel.CLASSIC;
		try {
			return Optional.of(new PlayerSkin(URI.create(url.group(1).replace("\\/", "/")).toURL(), model));
		} catch (IllegalArgumentException | java.net.MalformedURLException exception) {
			return Optional.empty();
		}
	}
}
