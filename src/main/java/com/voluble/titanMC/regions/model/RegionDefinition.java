package com.voluble.titanMC.regions.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RegionDefinition(
	RegionId id,
	RegionKey key,
	WorldId worldId,
	int priority,
	List<BlockBox> boxes,
	Instant createdAt,
	Instant updatedAt
) {

	public RegionDefinition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(worldId, "worldId");
		Objects.requireNonNull(createdAt, "createdAt");
		Objects.requireNonNull(updatedAt, "updatedAt");
		boxes = List.copyOf(boxes);
		if (boxes.isEmpty()) throw new IllegalArgumentException("region must contain at least one box");
		if (boxes.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("boxes must not contain null");
		if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("updatedAt must not precede createdAt");
	}

	public static RegionDefinition create(RegionKey key, WorldId worldId, int priority, List<BlockBox> boxes) {
		Instant now = Instant.now();
		return new RegionDefinition(RegionId.random(), key, worldId, priority, boxes, now, now);
	}

	public boolean contains(int x, int y, int z) {
		return boxes.stream().anyMatch(box -> box.contains(x, y, z));
	}

	public boolean intersects(BlockBox box) {
		return boxes.stream().anyMatch(candidate -> candidate.intersects(box));
	}
}
