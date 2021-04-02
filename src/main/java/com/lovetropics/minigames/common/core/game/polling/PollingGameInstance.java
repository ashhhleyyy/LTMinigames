package com.lovetropics.minigames.common.core.game.polling;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.minigame.ClientRoleMessage;
import com.lovetropics.minigames.client.minigame.PlayerCountsMessage;
import com.lovetropics.minigames.common.core.game.*;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorMap;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePollingEvents;
import com.lovetropics.minigames.common.core.game.control.GameControlCommands;
import com.lovetropics.minigames.common.core.game.map.GameMap;
import com.lovetropics.minigames.common.core.game.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.network.LoveTropicsNetwork;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class PollingGameInstance implements ProtoGameInstance {
	private final String instanceId;
	private final MinecraftServer server;
	private final IGameDefinition definition;
	private final PlayerKey initiator;

	private final BehaviorMap behaviors;
	private final GameEventListeners events = new GameEventListeners();
	private final GameControlCommands controlCommands;

	private final GameRegistrations registrations = new GameRegistrations();

	private PollingGameInstance(String instanceId, MinecraftServer server, IGameDefinition definition, PlayerKey initiator) {
		this.instanceId = instanceId;
		this.server = server;
		this.definition = definition;
		this.behaviors = definition.createBehaviors();
		this.initiator = initiator;

		this.controlCommands = new GameControlCommands(initiator);
	}

	public static GameResult<PollingGameInstance> create(MinecraftServer server, IGameDefinition definition, PlayerKey initiator) {
		String instanceId = generateInstanceId(definition);

		PollingGameInstance instance = new PollingGameInstance(instanceId, server, definition, initiator);

		for (IGameBehavior behavior : instance.behaviors) {
			try {
				behavior.registerPolling(instance, instance.events);
			} catch (GameException e) {
				return GameResult.error(e.getTextMessage());
			}
		}

		return GameResult.ok(instance);
	}

	private static String generateInstanceId(IGameDefinition definition) {
		return definition.getDisplayId().getPath() + "_" + RandomStringUtils.randomAlphanumeric(5);
	}

	@Override
	public String getInstanceId() {
		return instanceId;
	}

	@Override
	public GameStatus getStatus() {
		return GameStatus.POLLING;
	}

	@Override
	public boolean requestPlayerJoin(ServerPlayerEntity player, @Nullable PlayerRole requestedRole) {
		if (registrations.contains(player.getUniqueID())) {
			return false;
		}

		try {
			invoker(GamePollingEvents.PLAYER_REGISTER).onPlayerRegister(this, player, requestedRole);
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player register event", e);
		}

		registrations.add(player.getUniqueID(), requestedRole);

		PlayerSet serverPlayers = PlayerSet.ofServer(server);
		GameMessages gameMessages = GameMessages.forGame(definition);

		if (registrations.participantCount() == definition.getMinimumParticipantCount()) {
			serverPlayers.sendMessage(gameMessages.enoughPlayers());
		}

		serverPlayers.sendMessage(gameMessages.playerJoined(player, requestedRole));

		PlayerRole trueRole = requestedRole == null ? PlayerRole.PARTICIPANT : requestedRole;
		LoveTropicsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientRoleMessage(trueRole));
		LoveTropicsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PlayerCountsMessage(trueRole, getMemberCount(trueRole)));

		return true;
	}

	@Override
	public boolean removePlayer(ServerPlayerEntity player) {
		if (!registrations.remove(player.getUniqueID())) {
			return false;
		}

		GameMessages gameMessages = GameMessages.forGame(definition);
		if (registrations.participantCount() == definition.getMinimumParticipantCount() - 1) {
			PlayerSet.ofServer(server).sendMessage(gameMessages.noLongerEnoughPlayers());
		}

		LoveTropicsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientRoleMessage(null));
		for (PlayerRole role : PlayerRole.ROLES) {
			LoveTropicsNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PlayerCountsMessage(role, getMemberCount(role)));
		}

		return true;
	}

	public CompletableFuture<GameResult<GameInstance>> start() {
		return definition.getMapProvider().open(server)
				.thenComposeAsync(result -> {
					if (result.isOk()) {
						GameMap map = result.getOk();
						return GameInstance.start(instanceId, server, definition, map, behaviors, initiator, registrations);
					} else {
						return CompletableFuture.completedFuture(result.castError());
					}
				}, server)
				.handle((result, throwable) -> {
					if (throwable instanceof Exception) {
						return GameResult.fromException("Unknown error starting minigame", (Exception) throwable);
					}
					return result;
				});
	}

	@Override
	public MinecraftServer getServer() {
		return server;
	}

	@Override
	public IGameDefinition getDefinition() {
		return definition;
	}

	@Override
	public BehaviorMap getBehaviors() {
		return behaviors;
	}

	@Override
	public GameControlCommands getControlCommands() {
		return controlCommands;
	}

	@Override
	public GameEventListeners getEvents() {
		return this.events;
	}

	@Override
	public int getMemberCount(PlayerRole role) {
		// TODO extensible
		return role == PlayerRole.PARTICIPANT ? registrations.participantCount() : registrations.spectatorCount();
	}

	public boolean isPlayerRegistered(ServerPlayerEntity player) {
		return registrations.contains(player.getUniqueID());
	}
}
