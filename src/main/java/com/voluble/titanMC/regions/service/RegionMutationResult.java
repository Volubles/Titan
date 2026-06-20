package com.voluble.titanMC.regions.service;

import com.voluble.titanMC.regions.model.RegionDefinition;

public sealed interface RegionMutationResult permits RegionMutationResult.Success, RegionMutationResult.Failure {

	boolean successful();

	record Success(RegionDefinition region) implements RegionMutationResult {
		@Override
		public boolean successful() {
			return true;
		}
	}

	record Failure(Reason reason, String message) implements RegionMutationResult {
		@Override
		public boolean successful() {
			return false;
		}
	}

	enum Reason {
		NOT_FOUND,
		STALE_REVISION,
		DUPLICATE_KEY,
		INVALID_GEOMETRY,
		STORAGE_FAILURE,
		ENGINE_CLOSED,
		QUEUE_FULL,
		ENGINE_UNHEALTHY,
		INTERNAL_CONFLICT
	}
}
