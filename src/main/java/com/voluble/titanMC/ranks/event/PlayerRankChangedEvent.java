package com.voluble.titanMC.ranks.event;

import com.voluble.titanMC.ranks.model.PlayerRank;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerRankChangedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final UUID playerId;
	private final PlayerRank previous;
	private final PlayerRank current;
	private final PlayerRankChangeCause cause;

	public PlayerRankChangedEvent(UUID playerId, @Nullable PlayerRank previous, PlayerRank current) {
		this(playerId, previous, current, PlayerRankChangeCause.ADMIN);
	}

	public PlayerRankChangedEvent(
		UUID playerId,
		@Nullable PlayerRank previous,
		PlayerRank current,
		PlayerRankChangeCause cause
	) {
		this.playerId = Objects.requireNonNull(playerId, "playerId");
		this.previous = previous;
		this.current = Objects.requireNonNull(current, "current");
		this.cause = Objects.requireNonNull(cause, "cause");
		if (!current.playerId().equals(playerId)) {
			throw new IllegalArgumentException("current rank player id does not match event player id");
		}
		if (previous != null && !previous.playerId().equals(playerId)) {
			throw new IllegalArgumentException("previous rank player id does not match event player id");
		}
	}

	public UUID playerId() {
		return playerId;
	}

	public Optional<PlayerRank> previous() {
		return Optional.ofNullable(previous);
	}

	public PlayerRank current() {
		return current;
	}

	public PlayerRankChangeCause cause() {
		return cause;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
