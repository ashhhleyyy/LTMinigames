package com.lovetropics.minigames.common.core.game.behavior.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.List;

public final class GameWorldEvents {
	public static final GameEventType<ChunkLoad> CHUNK_LOAD = GameEventType.create(ChunkLoad.class, listeners -> (chunk) -> {
		for (ChunkLoad listener : listeners) {
			listener.onChunkLoad(chunk);
		}
	});

	public static final GameEventType<ExplosionDetonate> EXPLOSION_DETONATE = GameEventType.create(ExplosionDetonate.class, listeners -> (explosion, affectedBlocks, affectedEntities) -> {
		for (ExplosionDetonate listener : listeners) {
			listener.onExplosionDetonate(explosion, affectedBlocks, affectedEntities);
		}
	});

	public static final GameEventType<SaplingGrow> SAPLING_GROW = GameEventType.create(SaplingGrow.class, listeners -> (world, pos) -> {
		for (SaplingGrow listener : listeners) {
			InteractionResult result = listener.onSaplingGrow(world, pos);

			if (result != InteractionResult.PASS) {
				return result;
			}
		}

		return InteractionResult.PASS;
	});

	private GameWorldEvents() {
	}

	public interface ChunkLoad {
		void onChunkLoad(ChunkAccess chunk);
	}

	public interface ExplosionDetonate {
		void onExplosionDetonate(Explosion explosion, List<BlockPos> affectedBlocks, List<Entity> affectedEntities);
	}

	public interface SaplingGrow {
		InteractionResult onSaplingGrow(Level world, BlockPos pos);
	}
}
