package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public record ImmediateRespawnBehavior(Optional<PlayerRole> role, Optional<PlayerRole> respawnAsRole, Optional<TemplatedText> deathMessage, boolean dropInventory) implements IGameBehavior {
	public static final Codec<ImmediateRespawnBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			PlayerRole.CODEC.optionalFieldOf("role").forGetter(c -> c.role),
			PlayerRole.CODEC.optionalFieldOf("respawn_as").forGetter(c -> c.respawnAsRole),
			TemplatedText.CODEC.optionalFieldOf("death_message").forGetter(c -> c.deathMessage),
			Codec.BOOL.optionalFieldOf("drop_inventory", false).forGetter(c -> c.dropInventory)
	).apply(i, ImmediateRespawnBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.DEATH, (player, source) -> onPlayerDeath(game, player));
	}

	private InteractionResult onPlayerDeath(IGamePhase game, ServerPlayer player) {
		player.getInventory().dropAll();

		PlayerRole playerRole = game.getRoleFor(player);
		if (this.role.isEmpty() || this.role.get() == playerRole) {
			this.respawnPlayer(game, player, playerRole);
			this.sendDeathMessage(game, player);

			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	private void respawnPlayer(IGamePhase game, ServerPlayer player, PlayerRole playerRole) {
		if (respawnAsRole.isPresent()) {
			game.setPlayerRole(player, respawnAsRole.get());
		} else {
			game.invoker(GamePlayerEvents.SPAWN).onSpawn(player, playerRole);
		}

		player.setHealth(20.0F);
	}

	private void sendDeathMessage(IGamePhase game, ServerPlayer player) {
		if (deathMessage.isPresent()) {
			Component message = deathMessage.get().apply(player.getCombatTracker().getDeathMessage());
			game.getAllPlayers().sendMessage(message);
		}
	}
}
