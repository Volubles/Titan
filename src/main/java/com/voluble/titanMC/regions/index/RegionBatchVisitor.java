package com.voluble.titanMC.regions.index;

import com.voluble.titanMC.regions.model.BlockPosition;
import com.voluble.titanMC.regions.model.RegionDefinition;

@FunctionalInterface
public interface RegionBatchVisitor {

	RegionVisitResult visit(BlockPosition position, RegionDefinition region);
}
