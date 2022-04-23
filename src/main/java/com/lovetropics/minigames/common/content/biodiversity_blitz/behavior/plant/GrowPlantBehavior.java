package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantType;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public final class GrowPlantBehavior implements IGameBehavior {
	public static final Codec<GrowPlantBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("time").forGetter(c -> c.time),
			PlantType.CODEC.fieldOf("grow_into").forGetter(c -> c.growInto)
	).apply(instance, GrowPlantBehavior::new));

	private final int time;
	private final PlantType growInto;

	private final List<Plant> growPlants = new ArrayList<>();

	public GrowPlantBehavior(int time, PlantType growInto) {
		this.time = time;
		this.growInto = growInto;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(BbPlantEvents.ADD, (player, plot, plant) -> {
			plant.state().put(GrowTime.KEY, new GrowTime(game.ticks() + this.time));
		});

		events.listen(BbPlantEvents.TICK, (player, plot, plants) -> {
			long ticks = game.ticks();
			if (ticks % 20 != 0) return;

			List<Plant> growPlants = this.collectPlantsToGrow(plants, ticks);
			for (Plant plant : growPlants) {
				this.tryGrowPlant(game, player, plot, plant);
			}
		});
	}

	private void tryGrowPlant(IGamePhase game, ServerPlayerEntity player, Plot plot, Plant plant) {
		ServerWorld world = game.getWorld();

		PlantSnapshot snapshot = this.removeAndSnapshot(world, plot, plant);

		BlockPos origin = plant.coverage().getOrigin();
		ActionResult<Plant> result = game.invoker(BbEvents.PLACE_PLANT).placePlant(player, plot, origin, this.growInto);
		if (result.getResult() != ActionResultType.SUCCESS) {
			this.restoreSnapshot(world, plot, snapshot);

			GrowTime growTime = plant.state(GrowTime.KEY);
			if (growTime != null) {
				growTime.next = game.ticks() + this.time;
			}
		}
	}

	private List<Plant> collectPlantsToGrow(List<Plant> plants, long ticks) {
		List<Plant> result = this.growPlants;
		result.clear();

		for (Plant plant : plants) {
			GrowTime growTime = plant.state(GrowTime.KEY);
			if (growTime != null && ticks >= growTime.next) {
				result.add(plant);
			}
		}

		return result;
	}

	private PlantSnapshot removeAndSnapshot(ServerWorld world, Plot plot, Plant plant) {
		plot.plants.removePlant(plant);

		Long2ObjectMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
		for (BlockPos pos : plant.coverage()) {
			blocks.put(pos.asLong(), world.getBlockState(pos));
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), Constants.BlockFlags.BLOCK_UPDATE | Constants.BlockFlags.UPDATE_NEIGHBORS);
		}

		return new PlantSnapshot(plant, blocks);
	}

	private void restoreSnapshot(ServerWorld world, Plot plot, PlantSnapshot snapshot) {
		plot.plants.addPlant(snapshot.plant);

		for (BlockPos pos : snapshot.plant.coverage()) {
			BlockState block = snapshot.blocks.get(pos.asLong());
			if (block != null) {
				world.setBlock(pos, block, Constants.BlockFlags.BLOCK_UPDATE | Constants.BlockFlags.UPDATE_NEIGHBORS);
			}
		}
	}

	static final class GrowTime {
		static final PlantState.Key<GrowTime> KEY = PlantState.Key.create();

		long next;

		GrowTime(long next) {
			this.next = next;
		}
	}

	static final class PlantSnapshot {
		final Plant plant;
		final Long2ObjectMap<BlockState> blocks;

		PlantSnapshot(Plant plant, Long2ObjectMap<BlockState> blocks) {
			this.plant = plant;
			this.blocks = blocks;
		}
	}
}
