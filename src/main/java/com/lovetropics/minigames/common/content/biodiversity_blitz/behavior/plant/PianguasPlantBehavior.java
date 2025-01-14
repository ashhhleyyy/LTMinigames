package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

public final class PianguasPlantBehavior implements IGameBehavior {
    public static final Codec<PianguasPlantBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("radius").forGetter(b -> b.radius),
            MoreCodecs.BLOCK_STATE.fieldOf("block").forGetter(c -> c.state)
    ).apply(i, PianguasPlantBehavior::new));
    private static final TagKey<Block> MUD = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("tropicraft", "mud"));
    private final int radius;
    private final BlockState state;

    private IGamePhase game;

    public PianguasPlantBehavior(int radius, BlockState state) {
        this.radius = radius;
        this.state = state;
    }

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        this.game = game;
        events.listen(BbPlantEvents.TICK, this::tickPlants);
    }

    private void tickPlants(ServerPlayer player, Plot plot, List<Plant> plants) {
        long ticks = this.game.ticks();
        Random random = this.game.getWorld().getRandom();

        // TODO: rebalance
        if (ticks % 300 != 0) {
            return;
        }

        ServerLevel world = this.game.getWorld();

        for (Plant plant : plants) {
            int dx = random.nextInt(this.radius) - random.nextInt(this.radius);
            int dz = random.nextInt(this.radius) - random.nextInt(this.radius);

            BlockPos check = plant.coverage().getOrigin().offset(dx, -1, dz);

            if (world.getBlockState(check).is(MUD)) {
                world.setBlockAndUpdate(check, state);
            }
        }
    }
}
