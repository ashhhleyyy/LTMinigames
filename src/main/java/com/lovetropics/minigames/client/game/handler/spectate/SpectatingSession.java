package com.lovetropics.minigames.client.game.handler.spectate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import java.util.List;
import java.util.UUID;

class SpectatingSession {
	private static final Minecraft CLIENT = Minecraft.getInstance();

	List<UUID> players;

	final SpectatingUi ui;

	SpectatingState state;
	SpectatingState.StateApplicator stateApplicator;

	double targetZoom = 1.0;
	double zoom = 1.0;
	double prevZoom = 1.0;

	SpectatingSession(List<UUID> players) {
		this.players = players;
		this.ui = new SpectatingUi(this);
		this.applyState(SpectatingState.FREE_CAMERA);
	}

	void tick() {
		prevZoom = zoom;
		zoom = targetZoom;

		if (stateApplicator != null) {
			if (stateApplicator.tryApply(CLIENT)) {
				stateApplicator = null;
			} else {
				return;
			}
		}

		SpectatingState newState = state.tick(CLIENT, this, CLIENT.player);
		if (!newState.equals(state)) {
			applyState(newState);
			ui.updateState(newState);
		}
	}

	double getZoom(float partialTicks) {
		return MathHelper.lerp(partialTicks, prevZoom, zoom);
	}

	void renderTick() {
		state.renderTick(CLIENT, this, CLIENT.player);
	}

	void applyToCamera(ActiveRenderInfo camera, float partialTicks, EntityViewRenderEvent.CameraSetup event) {
		state.applyToCamera(CLIENT, this, CLIENT.player, camera, partialTicks, event);
	}

	void applyState(SpectatingState state) {
		this.state = state;
		this.stateApplicator = state.apply(CLIENT, this);
	}

	void updatePlayers(List<UUID> players) {
		this.players = players;
		ui.updatePlayers(players);
	}

	void close() {
		applyState(SpectatingState.FREE_CAMERA);
	}
}