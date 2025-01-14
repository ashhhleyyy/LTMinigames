package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantHealth;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantNotPathfindable;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public record PlantHealthBehavior(int health, boolean notPathfindable) implements IGameBehavior {
	public static final Codec<PlantHealthBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("health").forGetter(b -> b.health),
			Codec.BOOL.optionalFieldOf("not_pathfindable", false).forGetter(b -> b.notPathfindable)
	).apply(i, PlantHealthBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(BbPlantEvents.ADD, (player, plot, plant) -> {
			plant.state().put(PlantHealth.KEY, new PlantHealth(this.health));
			if (this.notPathfindable) {
				plant.state().put(PlantNotPathfindable.KEY, new PlantNotPathfindable());
			}
		});

		events.listen(BbPlantEvents.TICK, (player, plot, plants) -> {
			ServerLevel world = game.getWorld();

			List<Plant> decayedPlants = new ArrayList<>();

			for (Plant plant : plants) {
				PlantHealth health = plant.state(PlantHealth.KEY);
				if (health == null) {
					continue;
				}

				// TODO: this doesn't work because the level renderer only tracks 1 destroy progress per player
				player.getLevel().destroyBlockProgress(player.getId(), plant.coverage().getOrigin(), (int)(health.healthPercent() * 10.0F) - 1);

				if (health.isDead()) {
					for (BlockPos pos : plant.coverage()) {
						BlockState state = world.getBlockState(pos);
						world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
					}

					decayedPlants.add(plant);
				}
			}

			decayedPlants.forEach(plant -> game.invoker(BbEvents.BREAK_PLANT).breakPlant(player, plot, plant));
		});
	}
}
