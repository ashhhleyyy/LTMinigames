package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.DebugModeState;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.mojang.serialization.Codec;

public final class DebugModeBehavior implements IGameBehavior {
	public static final Codec<DebugModeBehavior> CODEC = Codec.unit(DebugModeBehavior::new);

	@Override
	public void registerState(IGamePhase game, GameStateMap state) {
		state.register(DebugModeState.KEY, new DebugModeState());
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
	}
}
