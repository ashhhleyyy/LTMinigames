package com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantState;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.stream.Stream;

public final class Plant {
	private final PlantType type;
	private final PlantCoverage functionalCoverage;
	@Nullable
	private final PlantCoverage decorationCoverage;
	private final PlantState state;

	private final PlantCoverage coverage;
	private final PlantFamily family;
	private final double value;

	public Plant(PlantType type, PlantCoverage functionalCoverage, @Nullable PlantCoverage decorationCoverage, PlantFamily family, double value) {
		this(type, functionalCoverage, decorationCoverage, new PlantState(), family, value);
	}

	private Plant(PlantType type, PlantCoverage functionalCoverage, @Nullable PlantCoverage decorationCoverage, PlantState state, PlantFamily family, double value) {
		this.type = type;
		this.functionalCoverage = functionalCoverage;
		this.decorationCoverage = decorationCoverage;
		this.state = state;

		this.coverage = decorationCoverage != null ? PlantCoverage.or(functionalCoverage, decorationCoverage) : functionalCoverage;
		this.family = family;
		this.value = value;
	}

	public PlantType type() {
		return type;
	}

	public PlantCoverage functionalCoverage() {
		return functionalCoverage;
	}

	@Nullable
	public PlantCoverage decorationCoverage() {
		return decorationCoverage;
	}

	public PlantCoverage coverage() {
		return coverage;
	}

	public PlantState state() {
		return state;
	}

	public PlantFamily family() {
		return this.family;
	}

	public double value() {
		return this.value;
	}

	@Nullable
	public <S> S state(PlantState.Key<S> key) {
		return state.get(key);
	}

	public void spawnPoof(ServerLevel world) {
		spawnPoof(world, 20, 0.15);
	}

	public void spawnPoof(ServerLevel world, int count, double speed) {
		Random random = world.random;

		for (BlockPos pos : this.coverage) {
			for (int i = 0; i < count; i++) {
				double vx = random.nextGaussian() * 0.02;
				double vy = random.nextGaussian() * 0.02;
				double vz = random.nextGaussian() * 0.02;
				world.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1, vx, vy, vz, speed);
			}
		}
	}

	public Stream<BlockPos> getBlockPositions(ServerLevel world, Block block) {
		return coverage().stream()
				.peek(bp -> System.out.println(world.getBlockState(bp)))
				.filter(bp -> world.getBlockState(bp).getBlock() == block);
	}
}
