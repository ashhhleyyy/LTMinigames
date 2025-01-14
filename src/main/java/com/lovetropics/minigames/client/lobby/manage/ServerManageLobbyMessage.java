package com.lovetropics.minigames.client.lobby.manage;

import com.lovetropics.minigames.client.lobby.manage.state.update.ServerLobbyUpdate;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.lobby.IGameLobby;
import com.lovetropics.minigames.common.core.game.lobby.ILobbyManagement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class ServerManageLobbyMessage {
	private final int id;
	@Nullable
	private final ServerLobbyUpdate.Set updates;

	private ServerManageLobbyMessage(int id, @Nullable ServerLobbyUpdate.Set updates) {
		this.id = id;
		this.updates = updates;
	}

	public static ServerManageLobbyMessage update(int id, ServerLobbyUpdate.Set updates) {
		return new ServerManageLobbyMessage(id, updates);
	}

	public static ServerManageLobbyMessage stop(int id) {
		return new ServerManageLobbyMessage(id, null);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(id);
		buffer.writeBoolean(updates != null);
		if (updates != null) {
			updates.encode(buffer);
		}
	}

	public static ServerManageLobbyMessage decode(FriendlyByteBuf buffer) {
		int id = buffer.readVarInt();
		ServerLobbyUpdate.Set updates = buffer.readBoolean() ? ServerLobbyUpdate.Set.decode(buffer) : null;
		return new ServerManageLobbyMessage(id, updates);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			IGameLobby lobby = IGameManager.get().getLobbyByNetworkId(id);
			ServerPlayer player = ctx.get().getSender();
			if (lobby == null || player == null) return;

			ILobbyManagement management = lobby.getManagement();
			if (updates != null) {
				if (management.canManage(player.createCommandSourceStack())) {
					updates.applyTo(management);
				}
			} else {
				management.stopManaging(player);
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
