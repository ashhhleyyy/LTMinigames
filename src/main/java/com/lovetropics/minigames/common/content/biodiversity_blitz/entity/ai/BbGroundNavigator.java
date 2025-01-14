package com.lovetropics.minigames.common.content.biodiversity_blitz.entity.ai;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.BlockGetter;

public final class BbGroundNavigator extends GroundPathNavigation {
	private final BbMobEntity mob;

	public BbGroundNavigator(Mob mob) {
		super(mob, mob.level);
		this.mob = (BbMobEntity) mob;
	}

	@Override
	protected PathFinder createPathFinder(int maxDepth) {
		this.nodeEvaluator = new NodeProcessor();
		this.nodeEvaluator.setCanPassDoors(true);
		return new PathFinder(this.nodeEvaluator, maxDepth);
	}

	final class NodeProcessor extends WalkNodeEvaluator {
		@Override
		public BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z) {
			BbMobBrain brain = BbGroundNavigator.this.mob.getMobBrain();

			if (!brain.getPlotWalls().getBounds().contains(x + 0.5, y + 0.5, z + 0.5)) {
				return BlockPathTypes.BLOCKED;
			}

			BlockPathTypes nodeType = super.getBlockPathType(world, x, y, z);
			if (nodeType.getMalus() >= 0.0F && brain.isScaredAt(x, y, z)) {
				return BlockPathTypes.BLOCKED;
			}

			return nodeType;
		}
	}
}
