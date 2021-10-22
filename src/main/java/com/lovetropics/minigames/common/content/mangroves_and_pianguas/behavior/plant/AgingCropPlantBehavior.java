package com.lovetropics.minigames.common.content.mangroves_and_pianguas.behavior.plant;

import com.lovetropics.minigames.common.content.mangroves_and_pianguas.behavior.event.MpEvents;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public final class AgingCropPlantBehavior extends AgingPlantBehavior {
	public static final Codec<AgingCropPlantBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("interval").forGetter(c -> c.interval)
	).apply(instance, AgingCropPlantBehavior::new));

	public AgingCropPlantBehavior(int interval) {
		super(interval);
	}

	protected BlockState ageUp(Random random, BlockState state) {
		// Skip 50% of crops this tick
		if (random.nextInt(2) == 0) {
			return state;
		}

		if (state.hasProperty(BlockStateProperties.AGE_0_3)) {
			int age = state.get(BlockStateProperties.AGE_0_3);
			if (age < 3) {
				return state.with(BlockStateProperties.AGE_0_3, age + 1);
			}
		} else if (state.hasProperty(BlockStateProperties.AGE_0_7)) {
			int age = state.get(BlockStateProperties.AGE_0_7);
			if (age < 7) {
				return state.with(BlockStateProperties.AGE_0_7, age + 1);
			}
		}

		return state;
	}
}