package com.lovetropics.minigames.client.lobby.state;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorMap;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.Set;

public final class ClientBehaviorMap {
	public final Multimap<GameBehaviorType<?>, ClientConfigList> behaviors;

	public ClientBehaviorMap(Multimap<GameBehaviorType<?>, ClientConfigList> behaviors) {
		this.behaviors = behaviors;
	}

	public static ClientBehaviorMap from(BehaviorMap behaviors) {
		return new ClientBehaviorMap(Multimaps.transformValues(behaviors.mapValues(IGameBehavior::getConfigurables), ClientConfigList::from));
	}

	public static ClientBehaviorMap decode(FriendlyByteBuf buffer) {
		Multimap<GameBehaviorType<?>, ClientConfigList> behaviors = LinkedHashMultimap.create();
		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++) {
			GameBehaviorType<?> type = buffer.readRegistryIdUnsafe(GameBehaviorTypes.REGISTRY.get());
			int valueSize = buffer.readVarInt();
			for (int j = 0; j < valueSize; j++) {
				behaviors.put(type, ClientConfigList.decode(buffer));
			}
		}
		return new ClientBehaviorMap(behaviors);
	}

	public void encode(FriendlyByteBuf buffer) {
		Set<GameBehaviorType<?>> keys = behaviors.keySet();
		buffer.writeVarInt(keys.size());
		for (GameBehaviorType<?> k : keys) {
			buffer.writeRegistryIdUnsafe(GameBehaviorTypes.REGISTRY.get(), k);
			Collection<ClientConfigList> v = behaviors.get(k);
			buffer.writeVarInt(v.size());
			for (ClientConfigList list : v) {
				list.encode(buffer);
			}
		}
	}
}
