package com.voluble.titanMC.donatortools.tracking;

import org.bukkit.Location;

@FunctionalInterface
public interface BlockBreakTracker {

	void record(Location location);
}
