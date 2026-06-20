package com.voluble.titanMC.regions.service;

public record RegionEngineStats(
	RegionEngineHealth health,
	long snapshotVersion,
	int regionCount,
	int queuedMutations,
	int mutationQueueCapacity,
	long lastPublishedAtEpochMillis
) {}
