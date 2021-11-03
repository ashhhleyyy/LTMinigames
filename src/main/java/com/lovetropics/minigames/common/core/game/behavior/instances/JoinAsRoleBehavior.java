package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// TODO: rename this is not very clear!
public final class JoinAsRoleBehavior implements IGameBehavior {
	public static final Codec<JoinAsRoleBehavior> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
				PlayerRole.CODEC.fieldOf("role").forGetter(c -> c.role)
		).apply(instance, JoinAsRoleBehavior::new);
	});

	private final PlayerRole role;

	public JoinAsRoleBehavior(PlayerRole role) {
		this.role = role;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.JOIN, player -> game.setPlayerRole(player, role));
	}
}
