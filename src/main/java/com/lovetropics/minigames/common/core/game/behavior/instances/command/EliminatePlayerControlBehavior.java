package com.lovetropics.minigames.common.core.game.behavior.instances.command;

import com.lovetropics.minigames.common.core.game.state.instances.control.ControlCommand;
import com.lovetropics.minigames.common.core.game.IActiveGame;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.instances.control.ControlCommandState;
import com.mojang.serialization.Codec;
import net.minecraft.entity.player.ServerPlayerEntity;

public final class EliminatePlayerControlBehavior implements IGameBehavior {
	public static final Codec<EliminatePlayerControlBehavior> CODEC = Codec.unit(EliminatePlayerControlBehavior::new);

	@Override
	public void register(IActiveGame game, EventRegistrar events) {
		ControlCommandState commands = game.getState().get(ControlCommandState.TYPE);
		commands.add("eliminate", ControlCommand.forAdmins(source -> {
			ServerPlayerEntity player = source.asPlayer();
			if (!game.getSpectators().contains(player)) {
				game.setPlayerRole(player, PlayerRole.SPECTATOR);
				player.setHealth(20.0F);
			}
		}));
	}
}
