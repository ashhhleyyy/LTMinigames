package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.SerializableUUID;

import java.util.List;
import java.util.UUID;

public record SpectatingClientState(List<UUID> players) implements GameClientState {
	public static final Codec<SpectatingClientState> CODEC = RecordCodecBuilder.create(i -> i.group(
			SerializableUUID.CODEC.listOf().fieldOf("players").forGetter(c -> c.players)
	).apply(i, SpectatingClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SPECTATING.get();
	}
}
