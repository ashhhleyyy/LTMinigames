package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.CauseOfDeath;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;

public final class CauseOfDeathTrackerBehavior implements IGameBehavior {
	public static final Codec<CauseOfDeathTrackerBehavior> CODEC = Codec.unit(CauseOfDeathTrackerBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> onPlayerSetRole(game, player, role, lastRole));
		events.listen(GamePlayerEvents.DEATH, (player, source) -> onPlayerDeath(game, player, source));
	}

	private void onPlayerSetRole(IGamePhase game, ServerPlayer player, PlayerRole role, PlayerRole lastRole) {
		if (role == PlayerRole.PARTICIPANT) {
			game.getStatistics().forPlayer(player).set(StatisticKey.DEAD, false);
		} else if (role == PlayerRole.SPECTATOR && lastRole == PlayerRole.PARTICIPANT) {
			game.getStatistics().forPlayer(player).set(StatisticKey.DEAD, true);
		}
	}

	private InteractionResult onPlayerDeath(IGamePhase game, ServerPlayer player, DamageSource source) {
		StatisticsMap playerStatistics = game.getStatistics().forPlayer(player);

		playerStatistics.set(StatisticKey.CAUSE_OF_DEATH, CauseOfDeath.from(source));
		playerStatistics.set(StatisticKey.DEAD, true);

		return InteractionResult.PASS;
	}
}
