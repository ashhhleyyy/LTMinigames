package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.resources.ResourceLocation;

public record GiveLootAction(ResourceLocation lootTable) implements IGameBehavior {
	public static final Codec<GiveLootAction> CODEC = RecordCodecBuilder.create(i -> i.group(
			ResourceLocation.CODEC.fieldOf("loot_table").forGetter(c -> c.lootTable)
	).apply(i, GiveLootAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> addLootTableToInventory(player));
	}

	private boolean addLootTableToInventory(final ServerPlayer player) {
		LootContext context = (new LootContext.Builder(player.getLevel()))
				.withParameter(LootContextParams.THIS_ENTITY, player)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.withRandom(player.getRandom())
				.withLuck(player.getLuck())
				.create(LootContextParamSets.GIFT);

		boolean changed = false;
		for (ItemStack stack : player.server.getLootTables().get(lootTable).getRandomItems(context)) {
			if (Util.addItemStackToInventory(player, stack)) {
				changed = true;
			}
		}

		if (changed) {
			player.inventoryMenu.broadcastChanges();
			return true;
		} else {
			return false;
		}
	}
}
