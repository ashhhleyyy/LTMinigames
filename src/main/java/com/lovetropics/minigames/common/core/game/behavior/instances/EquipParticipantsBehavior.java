package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record EquipParticipantsBehavior(ItemStack[] equipment) implements IGameBehavior {
	public static final Codec<EquipParticipantsBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			MoreCodecs.arrayOrUnit(MoreCodecs.ITEM_STACK, ItemStack[]::new).fieldOf("equipment").forGetter(c -> c.equipment)
	).apply(i, EquipParticipantsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (role == PlayerRole.PARTICIPANT) {
				for (ItemStack item : equipment) {
					player.addItem(item.copy());
				}
			}
		});
	}
}
