package com.lovetropics.minigames.common.core.game.behavior.instances.team;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.config.BehaviorConfig;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameTeamEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public final class TeamsBehavior implements IGameBehavior {
	private static final BehaviorConfig<Boolean> CFG_FRIENDLY_FIRE = BehaviorConfig.fieldOf("friendly_fire", Codec.BOOL);

	public static final Codec<TeamsBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			CFG_FRIENDLY_FIRE.orElse(false).forGetter(c -> c.friendlyFire),
			Codec.BOOL.optionalFieldOf("static_team_ids", false).forGetter(c -> c.staticTeamIds)
	).apply(i, TeamsBehavior::new));

	private final Map<GameTeamKey, PlayerTeam> scoreboardTeams = new Object2ObjectOpenHashMap<>();

	private final boolean friendlyFire;
	private final boolean staticTeamIds;

	private TeamState teams;

	public TeamsBehavior(boolean friendlyFire, boolean staticTeamIds) {
		this.friendlyFire = friendlyFire;
		this.staticTeamIds = staticTeamIds;
	}

	@Override
	public ConfigList getConfigurables() {
		return ConfigList.builder()
				.with(CFG_FRIENDLY_FIRE, friendlyFire)
				.build();
	}

	@Override
	public IGameBehavior configure(ConfigList configs) {
		return new TeamsBehavior(CFG_FRIENDLY_FIRE.getValue(configs), staticTeamIds);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		teams = game.getState().getOrThrow(TeamState.KEY);

		this.addTeamsToScoreboard(game);

		events.listen(GamePhaseEvents.START, () -> {
			teams.getAllocations().allocate(game.getParticipants(), (player, team) -> {
				addPlayerToTeam(game, player, team);
			});
		});

		events.listen(GamePhaseEvents.DESTROY, () -> onDestroy(game));
		events.listen(GamePlayerEvents.ALLOCATE_ROLES, allocator -> reassignPlayerRoles(game, allocator));

		events.listen(GamePlayerEvents.SET_ROLE, this::onPlayerSetRole);
		events.listen(GamePlayerEvents.LEAVE, this::removePlayerFromTeams);
		events.listen(GamePlayerEvents.DAMAGE, this::onPlayerHurt);
		events.listen(GamePlayerEvents.ATTACK, this::onPlayerAttack);

		game.getStatistics().global().set(StatisticKey.TEAMS, true);
	}

	private void addTeamsToScoreboard(IGamePhase game) {
		MinecraftServer server = game.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		for (GameTeam team : teams) {
			String teamId = createTeamId(team.key());
			PlayerTeam scoreboardTeam = getOrCreateScoreboardTeam(scoreboard, team, teamId);
			scoreboardTeams.put(team.key(), scoreboardTeam);
		}
	}

	private PlayerTeam getOrCreateScoreboardTeam(ServerScoreboard scoreboard, GameTeam team, String teamId) {
		PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(teamId);
		if (scoreboardTeam == null) {
			scoreboardTeam = scoreboard.addPlayerTeam(teamId);
			scoreboardTeam.setDisplayName(team.config().name());
			scoreboardTeam.setColor(team.config().formatting());
			scoreboardTeam.setAllowFriendlyFire(friendlyFire);
		}

		return scoreboardTeam;
	}

	private String createTeamId(GameTeamKey team) {
		if (staticTeamIds) {
			return team.id();
		} else {
			return team.id() + "_" + RandomStringUtils.randomAlphabetic(3);
		}
	}

	private void reassignPlayerRoles(IGamePhase game, TeamAllocator<PlayerRole, ServerPlayer> allocator) {
		// force all assigned players to be a participant
		for (UUID uuid : teams.getAssignedPlayers()) {
			ServerPlayer player = game.getAllPlayers().getPlayerBy(uuid);
			if (player != null) {
				allocator.addPlayer(player, PlayerRole.PARTICIPANT);
			}
		}
	}

	private void onDestroy(IGamePhase game) {
		ServerScoreboard scoreboard = game.getServer().getScoreboard();
		for (PlayerTeam team : scoreboardTeams.values()) {
			scoreboard.removePlayerTeam(team);
		}
	}

	private void onPlayerSetRole(ServerPlayer player, @Nullable PlayerRole role, @Nullable PlayerRole lastRole) {
		if (lastRole == PlayerRole.PARTICIPANT && role != PlayerRole.PARTICIPANT) {
			removePlayerFromTeams(player);
		}
	}

	private void addPlayerToTeam(IGamePhase game, ServerPlayer player, GameTeamKey teamKey) {
		GameTeam team = teams.getTeamByKey(teamKey);
		if (team == null) {
			return;
		}

		teams.addPlayerTo(player, teamKey);

		game.invoker(GameTeamEvents.SET_GAME_TEAM).onSetGameTeam(player, teams, teamKey);

		game.getStatistics().forPlayer(player).set(StatisticKey.TEAM, teamKey);

		ServerScoreboard scoreboard = player.server.getScoreboard();
		PlayerTeam scoreboardTeam = scoreboardTeams.get(teamKey);
		scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboardTeam);

		Component teamName = team.config().name().copy().append(" Team!")
				.withStyle(ChatFormatting.BOLD, team.config().formatting());

		player.displayClientMessage(
				new TextComponent("You are on ").withStyle(ChatFormatting.GRAY).append(teamName),
				false
		);
	}

	private void removePlayerFromTeams(ServerPlayer player) {
		teams.removePlayer(player);

		ServerScoreboard scoreboard = player.server.getScoreboard();
		scoreboard.removePlayerFromTeam(player.getScoreboardName());
	}

	private InteractionResult onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
		if (!friendlyFire && teams.areSameTeam(source.getEntity(), player)) {
			return InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}

	private InteractionResult onPlayerAttack(ServerPlayer player, Entity target) {
		if (!friendlyFire && teams.areSameTeam(player, target)) {
			return InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}
}
