package com.voluble.titanMC.outfits.skin;

import com.voluble.titanMC.outfits.model.OutfitId;
import com.voluble.titanMC.outfits.model.SkinModel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MineSkinClient {
	private static final URI UPLOAD_ENDPOINT = URI.create("https://api.mineskin.org/v2/generate");
	private static final long MINIMUM_UPLOAD_SPACING_MILLIS = 3_000L;
	private static final int MAX_ATTEMPTS = 3;
	private static final Pattern VALUE = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern SIGNATURE = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern RETRY_AFTER = Pattern.compile("next request in (\\d+)ms");

	private final HttpClient client = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.build();
	private final Object uploadLock = new Object();
	private long nextUploadAtMillis;

	public SkinPropertyData upload(
		String apiKey,
		OutfitId outfitId,
		SkinModel model,
		MineSkinVisibility visibility,
		byte[] png
	) throws IOException, InterruptedException {
		Objects.requireNonNull(apiKey, "apiKey");
		Objects.requireNonNull(outfitId, "outfitId");
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(visibility, "visibility");
		Objects.requireNonNull(png, "png");
		synchronized (uploadLock) {
			for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
				waitForUploadSlot();
				HttpResponse<String> response = send(apiKey, outfitId, model, visibility, png);
				nextUploadAtMillis = System.currentTimeMillis() + MINIMUM_UPLOAD_SPACING_MILLIS;
				if (response.statusCode() >= 200 && response.statusCode() < 300) return property(response.body());
				if (response.statusCode() == 429 && attempt < MAX_ATTEMPTS) {
					nextUploadAtMillis = Math.max(nextUploadAtMillis, System.currentTimeMillis() + retryAfterMillis(response.body()));
					continue;
				}
				throw new IOException("MineSkin upload failed with HTTP " + response.statusCode() + ": " + trim(response.body()));
			}
			throw new IOException("MineSkin upload failed after " + MAX_ATTEMPTS + " attempts");
		}
	}

	private HttpResponse<String> send(
		String apiKey,
		OutfitId outfitId,
		SkinModel model,
		MineSkinVisibility visibility,
		byte[] png
	) throws IOException, InterruptedException {
		String boundary = "TitanMCBoundary" + System.nanoTime();
		byte[] body = multipart(boundary, outfitId, model, visibility, png);
		HttpRequest request = HttpRequest.newBuilder(UPLOAD_ENDPOINT)
			.timeout(Duration.ofSeconds(60))
			.header("Authorization", "Bearer " + apiKey)
			.header("Accept", "application/json")
			.header("User-Agent", "TitanMC-Outfits")
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.ofByteArray(body))
			.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private void waitForUploadSlot() throws InterruptedException {
		long wait = nextUploadAtMillis - System.currentTimeMillis();
		if (wait > 0L) Thread.sleep(wait);
	}

	private static long retryAfterMillis(String body) {
		Matcher matcher = RETRY_AFTER.matcher(body == null ? "" : body);
		if (!matcher.find()) return MINIMUM_UPLOAD_SPACING_MILLIS;
		try {
			return Math.max(MINIMUM_UPLOAD_SPACING_MILLIS, Long.parseLong(matcher.group(1)) + 250L);
		} catch (NumberFormatException exception) {
			return MINIMUM_UPLOAD_SPACING_MILLIS;
		}
	}

	private static byte[] multipart(
		String boundary,
		OutfitId outfitId,
		SkinModel model,
		MineSkinVisibility visibility,
		byte[] png
	) {
		String prefix = ""
			+ "--" + boundary + "\r\n"
			+ "Content-Disposition: form-data; name=\"name\"\r\n\r\n"
			+ "titanmc_" + outfitId.value() + "\r\n"
			+ "--" + boundary + "\r\n"
			+ "Content-Disposition: form-data; name=\"variant\"\r\n\r\n"
			+ (model == SkinModel.SLIM ? "slim" : "classic") + "\r\n"
			+ "--" + boundary + "\r\n"
			+ "Content-Disposition: form-data; name=\"visibility\"\r\n\r\n"
			+ visibility.apiName() + "\r\n"
			+ "--" + boundary + "\r\n"
			+ "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
			+ "Content-Type: image/png\r\n\r\n";
		String suffix = "\r\n--" + boundary + "--\r\n";
		byte[] before = prefix.getBytes(StandardCharsets.UTF_8);
		byte[] after = suffix.getBytes(StandardCharsets.UTF_8);
		byte[] body = new byte[before.length + png.length + after.length];
		System.arraycopy(before, 0, body, 0, before.length);
		System.arraycopy(png, 0, body, before.length, png.length);
		System.arraycopy(after, 0, body, before.length + png.length, after.length);
		return body;
	}

	private static SkinPropertyData property(String body) throws IOException {
		Matcher value = VALUE.matcher(body);
		Matcher signature = SIGNATURE.matcher(body);
		if (!value.find() || !signature.find()) {
			throw new IOException("MineSkin response did not contain a texture property");
		}
		return new SkinPropertyData(unescape(value.group(1)), unescape(signature.group(1)));
	}

	private static String unescape(String value) {
		return value.replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private static String trim(String value) {
		if (value == null) return "";
		return value.length() <= 500 ? value : value.substring(0, 500) + "...";
	}
}
