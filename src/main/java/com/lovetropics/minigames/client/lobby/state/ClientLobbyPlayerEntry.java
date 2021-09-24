package com.lovetropics.minigames.client.lobby.state;

import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ClientLobbyPlayerEntry {
	private final UUID uuid;
	@Nullable
	private final PlayerRole registeredRole;
	@Nullable
	private final PlayerRole currentRole;

	public ClientLobbyPlayerEntry(UUID uuid, @Nullable PlayerRole registeredRole, @Nullable PlayerRole currentRole) {
		this.uuid = uuid;
		this.registeredRole = registeredRole;
		this.currentRole = currentRole;
	}

	public void encode(PacketBuffer buffer) {
		buffer.writeUniqueId(this.uuid);
		encodeRole(buffer, this.registeredRole);
		encodeRole(buffer, this.currentRole);
	}

	public static ClientLobbyPlayerEntry decode(PacketBuffer buffer) {
		return new ClientLobbyPlayerEntry(
				buffer.readUniqueId(),
				decodeRole(buffer),
				decodeRole(buffer)
		);
	}

	private static void encodeRole(PacketBuffer buffer, @Nullable PlayerRole role) {
		if (role != null) {
			buffer.writeVarInt(role.ordinal() + 1);
		} else {
			buffer.writeVarInt(0);
		}
	}

	@Nullable
	private static PlayerRole decodeRole(PacketBuffer buffer) {
		int ordinal = buffer.readVarInt() - 1;
		if (ordinal >= 0 && ordinal < PlayerRole.values().length) {
			return PlayerRole.values()[ordinal];
		} else {
			return null;
		}
	}

	public UUID uuid() {
		return this.uuid;
	}

	@Nullable
	public PlayerRole registeredRole() {
		return this.registeredRole;
	}

	@Nullable
	public PlayerRole currentRole() {
		return this.currentRole;
	}
}
