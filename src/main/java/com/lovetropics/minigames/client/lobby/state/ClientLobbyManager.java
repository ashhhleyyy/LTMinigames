package com.lovetropics.minigames.client.lobby.state;

import com.lovetropics.minigames.Constants;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public final class ClientLobbyManager {
	private static final Int2ObjectMap<ClientLobbyState> LOBBIES = new Int2ObjectOpenHashMap<>();

	private static ClientLobbyState joinedLobby;

	@SubscribeEvent
	public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		clear();
	}

	public static Collection<ClientLobbyState> getLobbies() {
		return LOBBIES.values();
	}

	public static Optional<ClientLobbyState> get(int id) {
		return Optional.ofNullable(LOBBIES.get(id));
	}

	public static void setJoined(int id) {
		clearJoined();

		ClientLobbyState lobby = LOBBIES.get(id);
		if (lobby != null) {
			ClientLobbyManager.joinedLobby = lobby;
		}
	}

	public static void clearJoined() {
		ClientLobbyManager.joinedLobby = null;
	}

	@Nullable
	public static ClientLobbyState getJoined() {
		return joinedLobby;
	}

	public static ClientLobbyState addOrUpdate(int id, String name, @Nullable ClientCurrentGame currentGame) {
		ClientLobbyState lobby = LOBBIES.computeIfAbsent(id, ClientLobbyState::new);
		lobby.update(name, currentGame);
		return lobby;
	}

	public static void remove(int id) {
		LOBBIES.remove(id);

		ClientLobbyState joinedLobby = ClientLobbyManager.joinedLobby;
		if (joinedLobby != null && joinedLobby.id == id) {
			ClientLobbyManager.joinedLobby = null;
		}
	}

	public static void clear() {
		LOBBIES.clear();
		joinedLobby = null;
	}
}
