package com.lovetropics.minigames.common.minigames.behaviours.instances;

import com.lovetropics.minigames.common.map.MapRegion;
import com.lovetropics.minigames.common.minigames.IMinigameInstance;
import com.lovetropics.minigames.common.minigames.behaviours.IMinigameBehavior;
import com.mojang.datafixers.Dynamic;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerChunkProvider;

import java.util.Collection;

public final class ForceLoadRegionBehavior implements IMinigameBehavior {
	private final String regionKey;
	private LongSet acquiredChunks;

	public ForceLoadRegionBehavior(String regionKey) {
		this.regionKey = regionKey;
	}

	public static <T> ForceLoadRegionBehavior parse(Dynamic<T> root) {
		String region = root.get("region").asString("");
		return new ForceLoadRegionBehavior(region);
	}

	@Override
	public void onConstruct(IMinigameInstance minigame) {
		ServerChunkProvider chunkProvider = minigame.getWorld().getChunkProvider();

		LongSet chunks = collectChunks(minigame);

		LongIterator iterator = chunks.iterator();
		while (iterator.hasNext()) {
			long chunkKey = iterator.nextLong();
			chunkProvider.forceChunk(new ChunkPos(chunkKey), true);
		}

		acquiredChunks = chunks;
	}

	@Override
	public void onFinish(IMinigameInstance minigame) {
		ServerChunkProvider chunkProvider = minigame.getWorld().getChunkProvider();

		LongIterator iterator = acquiredChunks.iterator();
		while (iterator.hasNext()) {
			long chunkKey = iterator.nextLong();
			chunkProvider.forceChunk(new ChunkPos(chunkKey), false);
		}
	}

	private LongSet collectChunks(IMinigameInstance minigame) {
		LongSet chunks = new LongOpenHashSet();

		Collection<MapRegion> regions = minigame.getMapRegions().get(regionKey);
		for (MapRegion region : regions) {
			int minChunkX = region.min.getX() >> 4;
			int minChunkZ = region.min.getZ() >> 4;
			int maxChunkX = region.max.getX() >> 4;
			int maxChunkZ = region.max.getZ() >> 4;

			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
					chunks.add(ChunkPos.asLong(chunkX, chunkZ));
				}
			}
		}

		return chunks;
	}
}