package com.voluble.titanMC.regions.service;

import com.voluble.titanMC.regions.model.RegionDefinition;

import java.util.List;

public sealed interface RegionBatchResult permits RegionBatchResult.Success, RegionBatchResult.Failure {

	boolean successful();

	record Success(
		long snapshotVersion,
		List<RegionDefinition> saved,
		List<RegionDefinition> deleted
	) implements RegionBatchResult {
		public Success {
			saved = List.copyOf(saved);
			deleted = List.copyOf(deleted);
		}

		@Override
		public boolean successful() {
			return true;
		}
	}

	record Failure(int operationIndex, RegionMutationResult.Reason reason, String message) implements RegionBatchResult {
		@Override
		public boolean successful() {
			return false;
		}
	}
}
