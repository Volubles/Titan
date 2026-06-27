package com.voluble.titanMC.cinematics.model;

public sealed interface CinematicEvent permits CommandCinematicEvent, ParticleCinematicEvent, SoundCinematicEvent {
	int tick();

	int row();

	CinematicEventType type();
}
