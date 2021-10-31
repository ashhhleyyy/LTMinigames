package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class FlamingPlantBehavior implements IGameBehavior {
    public static final Codec<FlamingPlantBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("radius").forGetter(b -> b.radius)
    ).apply(instance, FlamingPlantBehavior::new));
    private final int radius;

    private IGamePhase game;

    public FlamingPlantBehavior(int radius) {
        this.radius = radius;
    }

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        this.game = game;
        events.listen(BbPlantEvents.TICK, this::tickPlants);
    }

    private void tickPlants(ServerPlayerEntity player, Plot plot, List<Plant> plants) {
        long ticks = this.game.ticks();
        Random random = this.game.getWorld().getRandom();

        if (ticks % 10 != 0) {
            return;
        }

        ServerWorld world = this.game.getWorld();

        Set<MobEntity> seen = new HashSet<>();

        for (Plant plant : plants) {
            AxisAlignedBB flameBounds = plant.coverage().asBounds().grow(this.radius);
            List<MobEntity> entities = world.getEntitiesWithinAABB(MobEntity.class, flameBounds, BbMobEntity.PREDICATE);

            int count = random.nextInt(3);
            if (!entities.isEmpty()) {
                for (MobEntity entity : entities) {
                    if (!seen.contains(entity)) {

                        seen.add(entity);

                        // In plant
                        if (entity.getPosition() == plant.coverage().getOrigin()) {
                            entity.setFire(6);

                            if (random.nextInt(3) == 0) {
                                entity.attackEntityFrom(DamageSource.IN_FIRE, 1 + random.nextInt(3));
                            }
                        } else {
                            entity.setFire(3);
                        }

                        AxisAlignedBB aabb = entity.getBoundingBox();

                        Vector3d positionVec = entity.getPositionVec();
                        // Needs to target the middle of the entity position vector
                        Vector3d scaledVec = new Vector3d(positionVec.x, (aabb.minY + aabb.maxY) / 2.0, positionVec.z);

                        Util.drawParticleBetween(ParticleTypes.FLAME, plant.coverage().asBounds().getCenter(), scaledVec, world, random, 10, 0.01, 0.02, 0.001, 0.01);
                    }
                }
            }

            // Add more particles if attacked entities
            count += (entities.size() > 0 ? 3 + random.nextInt(3) : 0);

            BlockPos pos = plant.coverage().getOrigin();
            if (random.nextInt(3) == 0) {

                for(int i = 0; i < count; ++i) {
                    double d3 = random.nextGaussian() * 0.02;
                    double d1 = random.nextGaussian() * 0.1;
                    double d2 = random.nextGaussian() * 0.02;

                    world.spawnParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1 + random.nextInt(2), d3, d1, d2, 0.002 + random.nextDouble() * random.nextDouble() * 0.025);
                }
            }
        }
    }
}
