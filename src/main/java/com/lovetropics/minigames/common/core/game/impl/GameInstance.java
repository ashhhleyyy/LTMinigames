package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGame;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

final class GameInstance implements IGame {
	final GameLobby lobby;
	final MinecraftServer server;
	final IGameDefinition definition;

	final GameStateMap stateMap = new GameStateMap();

	final UUID uuid = UUID.randomUUID();

	GameInstance(GameLobby lobby, IGameDefinition definition) {
		this.lobby = lobby;
		this.server = lobby.getServer();
		this.definition = definition;
	}

	@Override
	public IGameLobby getLobby() {
		return lobby;
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public IGameDefinition getDefinition() {
		return definition;
	}

	@Override
	public GameStateMap getState() {
		return stateMap;
	}
}
