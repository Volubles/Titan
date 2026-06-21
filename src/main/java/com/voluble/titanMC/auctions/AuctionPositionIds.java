package com.voluble.titanMC.auctions;

import com.voluble.titanMC.ranks.model.WardId;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class AuctionPositionIds {
	private AuctionPositionIds() {
	}

	public static String next(WardId wardId, Collection<String> existingIds) {
		Objects.requireNonNull(wardId, "wardId");
		Set<String> existing = new HashSet<>(Objects.requireNonNull(existingIds, "existingIds"));
		for (int index = 1; index < Integer.MAX_VALUE; index++) {
			String candidate = wardId.value() + "-" + String.format(java.util.Locale.ROOT, "%03d", index);
			if (!existing.contains(candidate)) return candidate;
		}
		throw new IllegalStateException("No auction position ids remain for ward " + wardId);
	}
}
