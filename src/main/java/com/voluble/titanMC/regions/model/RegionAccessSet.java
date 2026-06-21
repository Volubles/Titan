package com.voluble.titanMC.regions.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RegionAccessSet {

	private static final RegionAccessSet EMPTY = new RegionAccessSet(Set.of(), Set.of());

	private final Set<UUID> owners;
	private final Set<UUID> members;

	private RegionAccessSet(Set<UUID> owners, Set<UUID> members) {
		this.owners = Set.copyOf(owners);
		this.members = Set.copyOf(members);
	}

	public static RegionAccessSet empty() {
		return EMPTY;
	}

	public static RegionAccessSet of(Set<UUID> owners, Set<UUID> members) {
		Objects.requireNonNull(owners, "owners");
		Objects.requireNonNull(members, "members");
		LinkedHashSet<UUID> checkedOwners = checkedCopy(owners, "owners");
		LinkedHashSet<UUID> checkedMembers = checkedCopy(members, "members");
		checkedMembers.removeAll(checkedOwners);
		return checkedOwners.isEmpty() && checkedMembers.isEmpty()
			? EMPTY
			: new RegionAccessSet(checkedOwners, checkedMembers);
	}

	public Set<UUID> owners() {
		return owners;
	}

	public Set<UUID> members() {
		return members;
	}

	public boolean isOwner(UUID playerId) {
		return playerId != null && owners.contains(playerId);
	}

	public boolean isMember(UUID playerId) {
		return playerId != null && (owners.contains(playerId) || members.contains(playerId));
	}

	public RegionAccessSet withOwner(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		LinkedHashSet<UUID> updatedOwners = new LinkedHashSet<>(owners);
		updatedOwners.add(playerId);
		return of(updatedOwners, members);
	}

	public RegionAccessSet withoutOwner(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		LinkedHashSet<UUID> updatedOwners = new LinkedHashSet<>(owners);
		updatedOwners.remove(playerId);
		return of(updatedOwners, members);
	}

	public RegionAccessSet withMember(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		if (owners.contains(playerId)) return this;
		LinkedHashSet<UUID> updatedMembers = new LinkedHashSet<>(members);
		updatedMembers.add(playerId);
		return of(owners, updatedMembers);
	}

	public RegionAccessSet withoutMember(UUID playerId) {
		Objects.requireNonNull(playerId, "playerId");
		LinkedHashSet<UUID> updatedMembers = new LinkedHashSet<>(members);
		updatedMembers.remove(playerId);
		return of(owners, updatedMembers);
	}

	private static LinkedHashSet<UUID> checkedCopy(Set<UUID> values, String label) {
		LinkedHashSet<UUID> copy = new LinkedHashSet<>();
		for (UUID value : values) copy.add(Objects.requireNonNull(value, label + " must not contain null"));
		return copy;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RegionAccessSet access
			&& owners.equals(access.owners)
			&& members.equals(access.members);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owners, members);
	}

	@Override
	public String toString() {
		return "RegionAccessSet{owners=" + owners + ", members=" + members + '}';
	}
}
