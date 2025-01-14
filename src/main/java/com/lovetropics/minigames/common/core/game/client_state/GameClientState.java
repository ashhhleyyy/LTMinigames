package com.lovetropics.minigames.common.core.game.client_state;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;

public interface GameClientState {
	Codec<GameClientState> CODEC = GameClientStateTypes.TYPE_CODEC.dispatch(
			"type",
			GameClientState::getType,
			GameClientStateType::getCodec
	);

	GameClientStateType<?> getType();

	static void applyGlobally(GameClientState state, EventRegistrar events) {
		events.listen(GamePlayerEvents.ADD, player -> sendToPlayer(state, player));
		events.listen(GamePlayerEvents.REMOVE, player -> removeFromPlayer(state.getType(), player));
	}

	static void sendToPlayer(GameClientState state, ServerPlayer player) {
		GameClientStateSender.get().byPlayer(player).enqueueSet(state);
	}

	static void sendToPlayers(GameClientState state, PlayerSet players) {
		for (ServerPlayer player : players) {
			sendToPlayer(state, player);
		}
	}

	static void removeFromPlayer(GameClientStateType<?> type, ServerPlayer player) {
		GameClientStateSender.get().byPlayer(player).enqueueRemove(type);
	}

	static void removeFromPlayers(GameClientStateType<?> type, PlayerSet players) {
		for (ServerPlayer player : players) {
			removeFromPlayer(type, player);
		}
	}
}
