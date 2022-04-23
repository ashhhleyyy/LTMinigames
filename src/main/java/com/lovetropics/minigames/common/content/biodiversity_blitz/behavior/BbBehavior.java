package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.explosion.FilteredExplosion;
import com.lovetropics.minigames.common.content.biodiversity_blitz.explosion.PlantAffectingExplosion;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.CurrencyManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.dimension.DimensionUtils;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.*;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameType;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;

// TODO: needs to be split up & data-driven more!
public final class BbBehavior implements IGameBehavior {
	public static final Codec<BbBehavior> CODEC = Codec.unit(BbBehavior::new);

	private static final Object2FloatMap<Difficulty> DEATH_DECREASE = new Object2FloatOpenHashMap<>();

	static {
		DEATH_DECREASE.put(Difficulty.EASY, 0.9f);
		DEATH_DECREASE.put(Difficulty.NORMAL, 0.8F);
		DEATH_DECREASE.put(Difficulty.HARD, 0.5F);
	}

	private IGamePhase game;
	private PlotsState plots;
	private CurrencyManager currency;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		this.plots = game.getState().getOrThrow(PlotsState.KEY);
		this.currency = game.getState().getOrNull(CurrencyManager.KEY);

		events.listen(GamePlayerEvents.ADD, player -> setupPlayerAsRole(player, null));
		events.listen(GamePlayerEvents.SPAWN, this::setupPlayerAsRole);
		events.listen(BbEvents.ASSIGN_PLOT, this::onAssignPlot);
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
		events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
		events.listen(GameWorldEvents.EXPLOSION_DETONATE, this::onExplosion);
		// Don't grow any trees- we handle that ourselves
		events.listen(GameWorldEvents.SAPLING_GROW, (w, p) -> ActionResultType.FAIL);
		events.listen(GamePlayerEvents.ATTACK, this::onAttack);
		// No mob drops
		events.listen(GameLivingEntityEvents.MOB_DROP, (e, d, r) -> ActionResultType.FAIL);
		events.listen(GameLivingEntityEvents.FARMLAND_TRAMPLE, this::onTrampleFarmland);
		events.listen(GameEntityEvents.MOUNTED, (mounting, beingMounted) -> {
			if (mounting instanceof ServerPlayerEntity) {
				return ActionResultType.PASS;
			} else {
				return ActionResultType.FAIL;
			}
		});
		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
			Plot plot = this.plots.getPlotFor(player);
			if (plot == null) {
				return ActionResultType.PASS;
			}

			if (!plot.walls.getBounds().contains(player.position())) {
				return ActionResultType.FAIL;
			}

			return ActionResultType.PASS;
		});

		events.listen(GamePlayerEvents.PLACE_BLOCK, this::onPlaceBlock);
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> ActionResultType.FAIL);

		events.listen(GamePlayerEvents.USE_BLOCK, this::onUseBlock);
	}

	private ActionResultType onUseBlock(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, Hand hand, BlockRayTraceResult blockRayTraceResult) {
		Plot plot = this.plots.getPlotFor(player);
		BlockPos pos = blockRayTraceResult.getBlockPos();

		if (plot != null && plot.bounds.contains(pos)) {
			return this.onUseBlockInPlot(player, world, blockPos, hand, plot, pos);
		} else {
			return ActionResultType.FAIL;
		}
	}

	private ActionResultType onUseBlockInPlot(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, Hand hand, Plot plot, BlockPos pos) {
		// Check if farmland is being used and the user has a hoe TODO: can we make it not hardcoded?
		if (world.getBlockState(pos).getBlock() == Blocks.FARMLAND && player.getItemInHand(hand).getItem() instanceof HoeItem) {
			// If there is no plant above we can change to grass safely
			if (!plot.plants.hasPlantAt(pos.above())) {
				world.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
				world.playSound(null, blockPos, SoundEvents.HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
				player.swing(hand);
				player.getCooldowns().addCooldown(player.getItemInHand(hand).getItem(), 3);
				return ActionResultType.SUCCESS;
			}
		}

		return ActionResultType.PASS;
	}

	private ActionResultType onTrampleFarmland(Entity entity, BlockPos pos, BlockState state) {
		Plot plot = this.plots.getPlotFor(entity);
		if (plot != null && plot.bounds.contains(pos)) {
			if (!plot.plants.hasPlantAt(pos.above())) {
				return ActionResultType.PASS;
			}
		}

		return ActionResultType.FAIL;
	}

	private void setupPlayerAsRole(ServerPlayerEntity player, @Nullable PlayerRole role) {
		if (role == PlayerRole.SPECTATOR) {
			this.spawnSpectator(player);
		}
	}

	private void spawnSpectator(ServerPlayerEntity player) {
		Plot plot = plots.getRandomPlot(player.getRandom());
		if (plot != null) {
			teleportToRegion(player, plot.plantBounds, plot.forward);
		}

		player.setGameMode(GameType.SPECTATOR);
	}

	private void onAssignPlot(ServerPlayerEntity player, Plot plot) {
		teleportToRegion(player, plot.spawn, plot.spawnForward);
	}

	private void onExplosion(Explosion explosion, List<BlockPos> affectedBlocks, List<Entity> affectedEntities) {
		// Remove from filtered explosions
		if (explosion instanceof FilteredExplosion) {
			affectedEntities.removeIf(((FilteredExplosion)explosion).remove);
		}

		if (explosion instanceof PlantAffectingExplosion) {
			((PlantAffectingExplosion)explosion).affectPlants(affectedBlocks);
		}

		// Blocks should not explode
		affectedBlocks.clear();
	}

	private ActionResultType onAttack(ServerPlayerEntity player, Entity target) {
		if (BbMobEntity.matches(target)) {
			Plot plot = plots.getPlotAt(target.blockPosition());
			if (plot != null && plot.walls.containsEntity(player)) {
				return ActionResultType.PASS;
			}
		}
		return ActionResultType.FAIL;
	}

	private ActionResultType onPlaceBlock(ServerPlayerEntity player, BlockPos pos, BlockState placed, BlockState placedOn) {
		Plot plot = plots.getPlotFor(player);
		if (plot != null && plot.bounds.contains(pos)) {
			return this.onPlaceBlockInOwnPlot(player, pos, placed, plot);
		} else {
			this.sendActionRejection(player, BiodiversityBlitzTexts.notYourPlot());
			return ActionResultType.FAIL;
		}
	}

	private ActionResultType onPlaceBlockInOwnPlot(ServerPlayerEntity player, BlockPos pos, BlockState placed, Plot plot) {
		if (placed.is(Blocks.FARMLAND)) {
			player.level.setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
			return ActionResultType.PASS;
		}

		if (plot.plantBounds.contains(pos)) {
			this.sendActionRejection(player, BiodiversityBlitzTexts.canOnlyPlacePlants());
		}

		return ActionResultType.FAIL;
	}

	private void sendActionRejection(ServerPlayerEntity player, IFormattableTextComponent message) {
		player.displayClientMessage(message.withStyle(TextFormatting.RED), true);
		player.getLevel().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS,  1.0F, 1.0F);
	}

	private ActionResultType onPlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
		Plot plot = plots.getPlotFor(player);
		if (plot == null) {
			return ActionResultType.PASS;
		}

		teleportToRegion(player, plot.spawn, plot.spawnForward);
		player.setHealth(20.0F);
		if (player.getFoodData().getFoodLevel() < 10) {
			player.getFoodData().eat(2, 0.8f);
		}

		// TODO: this should really be in the currency behavior
		if (currency != null) {
			// Resets all currency from the player's inventory and adds a new stack with 80% of the amount.
			// A better way of just removing 20% of the existing stacks could be done but this was chosen for the time being to save time
			Difficulty difficulty = game.getWorld().getDifficulty();

			int oldCurrency = currency.get(player);
			int newCurrency = MathHelper.floor(oldCurrency * DEATH_DECREASE.getFloat(difficulty));

			if (oldCurrency != newCurrency) {
				currency.set(player, newCurrency, false);
		
	//			player.sendStatusMessage(BiodiversityBlitzTexts.deathDecrease(oldCurrency - newCurrency).mergeStyle(TextFormatting.RED), true);
		        player.connection.send(new STitlePacket(STitlePacket.Type.SUBTITLE, BiodiversityBlitzTexts.deathDecrease(oldCurrency - newCurrency).withStyle(TextFormatting.RED, TextFormatting.ITALIC)));
			}
		}

		player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.18F, 1.0F);
		player.addEffect(new EffectInstance(Effects.BLINDNESS, 80));
		player.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 255, 80));

		player.connection.send(new STitlePacket(40, 20, 0));
        player.connection.send(new STitlePacket(STitlePacket.Type.TITLE, BiodiversityBlitzTexts.deathTitle().withStyle(TextFormatting.RED)));

		return ActionResultType.FAIL;
	}

	private void tick(IGamePhase game) {
		for (ServerPlayerEntity player : game.getParticipants()) {
			Plot plot = plots.getPlotFor(player);
			if (plot != null) {
				game.invoker(BbEvents.TICK_PLOT).onTickPlot(player, plot);
			}
		}
	}

	private void teleportToRegion(ServerPlayerEntity player, BlockBox region, Direction direction) {
		BlockPos pos = region.sample(player.getRandom());

		player.yRot = direction.toYRot();
		DimensionUtils.teleportPlayerNoPortal(player, game.getDimension(), pos);
	}
}
