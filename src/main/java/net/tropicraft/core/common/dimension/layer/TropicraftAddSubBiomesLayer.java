package net.tropicraft.core.common.dimension.layer;

import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.IC1Transformer;

public enum TropicraftAddSubBiomesLayer implements IC1Transformer {
    // TODO add kelp forest here to sub biomes list
    RAINFOREST(TropicraftLayerUtil.RAINFOREST_PLAINS_ID, TropicraftLayerUtil.RAINFOREST_IDS)
    ;

    final int baseID;
    final int[] subBiomeIDs;

    TropicraftAddSubBiomesLayer(final int baseID, final int[] subBiomeIDs) {
        this.baseID = baseID;
        this.subBiomeIDs = subBiomeIDs;
    }

    @Override
    public int apply(INoiseRandom random, int center) {
        if (center == baseID) {
            return subBiomeIDs[random.random(subBiomeIDs.length)];
        } else {
            return center;
        }
    }
}