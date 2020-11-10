package com.lovetropics.minigames.common.minigames;

import com.lovetropics.minigames.common.dimension.DimensionUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.FoodStats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;

/**
 * Used to cache previous game type, dimension and position of player
 * before teleporting into minigame instance.
 * <p>
 * Can use restore() to reset them back to their previous state.
 */
public final class PlayerSnapshot {
	private final GameType gameType;
	private final DimensionType dimension;
	private final BlockPos pos;
	private final CompoundNBT playerData;

	private PlayerSnapshot(ServerPlayerEntity player) {
		this.gameType = player.interactionManager.getGameType();
		this.dimension = player.dimension;
		this.pos = player.getPosition();

		this.playerData = new CompoundNBT();
		player.writeAdditional(this.playerData);
	}

	public static PlayerSnapshot takeAndClear(ServerPlayerEntity player) {
		PlayerSnapshot snapshot = new PlayerSnapshot(player);
		clearPlayer(player);
		return snapshot;
	}

	private static void clearPlayer(ServerPlayerEntity player) {
		player.inventory.clear();
		player.setHealth(player.getMaxHealth());

		player.clearActivePotions();

		CompoundNBT foodTag = new CompoundNBT();
		new FoodStats().write(foodTag);
		player.getFoodStats().read(foodTag);
	}

	/**
	 * Resets the player back to their previous state when this cache
	 * was created.
	 *
	 * @param player The player being reset.
	 */
	public void restore(ServerPlayerEntity player) {
		clearPlayer(player);

		player.readAdditional(this.playerData);
		player.setGameType(this.gameType);

		DimensionUtils.teleportPlayerNoPortal(player, this.dimension, this.pos);
	}
}
