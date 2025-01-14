package com.lovetropics.minigames.common.core.game.state.team;

import com.google.common.base.Preconditions;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class TeamState implements IGameState, Iterable<GameTeam> {
	public static final GameStateKey<TeamState> KEY = GameStateKey.create("Teams");

	private final List<GameTeam> teams;

	private final Object2ObjectMap<GameTeamKey, GameTeam> teamsByKey = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectMap<GameTeamKey, MutablePlayerSet> playersByKey = new Object2ObjectOpenHashMap<>();

	private final Collection<GameTeam> pollingTeams;
	private final Set<UUID> assignedPlayers;

	private final Allocations allocations = new Allocations();

	public TeamState(List<GameTeam> teams) {
		this.teams = teams;

		this.pollingTeams = new ObjectOpenHashSet<>();
		this.assignedPlayers = new ObjectOpenHashSet<>();

		for (GameTeam team : teams) {
			this.teamsByKey.put(team.key(), team);

			List<UUID> assigned = team.config().assignedPlayers();
			if (!assigned.isEmpty()) {
				this.assignedPlayers.addAll(assigned);
			} else {
				this.pollingTeams.add(team);
			}
		}
	}

	public Allocations getAllocations() {
		return allocations;
	}

	public void addPlayerTo(ServerPlayer player, GameTeamKey team) {
		removePlayer(player);

		MutablePlayerSet players = getPlayersForTeamMutable(player.server, team);
		players.add(player);
	}

	@Nullable
	public GameTeamKey removePlayer(ServerPlayer player) {
		for (Map.Entry<GameTeamKey, MutablePlayerSet> entry : Object2ObjectMaps.fastIterable(playersByKey)) {
			if (entry.getValue().remove(player)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public PlayerSet getPlayersForTeam(GameTeamKey team) {
		PlayerSet players = playersByKey.get(team);
		return players != null ? players : PlayerSet.EMPTY;
	}

	private MutablePlayerSet getPlayersForTeamMutable(MinecraftServer server, GameTeamKey team) {
		MutablePlayerSet players = playersByKey.get(team);
		if (players == null) {
			Preconditions.checkState(teams.contains(getTeamByKey(team)), "invalid team " + team);
			players = new MutablePlayerSet(server);
			playersByKey.put(team, players);
		}
		return players;
	}

	@Nullable
	public GameTeamKey getTeamForPlayer(Player player) {
		for (Map.Entry<GameTeamKey, MutablePlayerSet> entry : Object2ObjectMaps.fastIterable(playersByKey)) {
			if (entry.getValue().contains(player)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public boolean isOnTeam(Player player, GameTeamKey team) {
		MutablePlayerSet players = playersByKey.get(team);
		return players != null && players.contains(player);
	}

	public Collection<GameTeamKey> getTeamKeys() {
		return teamsByKey.keySet();
	}

	public Collection<GameTeam> getPollingTeams() {
		return pollingTeams;
	}

	public Set<UUID> getAssignedPlayers() {
		return assignedPlayers;
	}

	@Nullable
	public GameTeam getTeamByKey(String key) {
		for (GameTeam team : teams) {
			if (team.key().id().equals(key)) {
				return team;
			}
		}
		return null;
	}

	@Nullable
	public GameTeam getTeamByKey(GameTeamKey key) {
		return teamsByKey.get(key);
	}

	public boolean areSameTeam(Entity source, Entity target) {
		if (!(source instanceof Player) || !(target instanceof Player)) {
			return false;
		}
		GameTeamKey sourceTeam = getTeamForPlayer((Player) source);
		GameTeamKey targetTeam = getTeamForPlayer((Player) target);
		return Objects.equals(sourceTeam, targetTeam);
	}

	@Override
	public Iterator<GameTeam> iterator() {
		return teams.iterator();
	}

	public final class Allocations {
		private final Map<UUID, GameTeamKey> preferences = new Object2ObjectOpenHashMap<>();

		public void allocate(PlayerSet participants, BiConsumer<ServerPlayer, GameTeamKey> apply) {
			// apply all direct team assignments first
			for (GameTeam team : teams) {
				List<UUID> assigned = team.config().assignedPlayers();
				for (UUID playerId : assigned) {
					ServerPlayer player = participants.getPlayerBy(playerId);
					if (player != null) {
						apply.accept(player, team.key());
					}
				}
			}

			if (!pollingTeams.isEmpty()) {
				TeamAllocator<GameTeamKey, ServerPlayer> teamAllocator = createAllocator();

				for (ServerPlayer player : participants) {
					UUID playerId = player.getUUID();
					if (!assignedPlayers.contains(playerId)) {
						teamAllocator.addPlayer(player, preferences.get(playerId));
					}
				}

				teamAllocator.allocate(apply);
			}
		}

		private TeamAllocator<GameTeamKey, ServerPlayer> createAllocator() {
			List<GameTeamKey> pollingTeamKeys = pollingTeams.stream().map(GameTeam::key).collect(Collectors.toList());
			TeamAllocator<GameTeamKey, ServerPlayer> teamAllocator = new TeamAllocator<>(pollingTeamKeys);

			for (GameTeam team : pollingTeams) {
				teamAllocator.setSizeForTeam(team.key(), team.config().maxSize());
			}

			return teamAllocator;
		}

		public void setPlayerPreference(UUID player, GameTeamKey team) {
			this.preferences.put(player, team);
		}
	}
}
