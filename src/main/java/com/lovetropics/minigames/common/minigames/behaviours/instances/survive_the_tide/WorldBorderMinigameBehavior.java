package com.lovetropics.minigames.common.minigames.behaviours.instances.survive_the_tide;

import com.lovetropics.minigames.common.minigames.IMinigameInstance;
import com.lovetropics.minigames.common.minigames.behaviours.IMinigameBehavior;
import com.mojang.datafixers.Dynamic;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;

public class WorldBorderMinigameBehavior implements IMinigameBehavior
{
	private final BlockPos worldBorderCenter;
	private final String collapseMessage;
	private final long ticksUntilStart;
	private final long delayUntilCollapse;
	private final int particleRateDelay;
	private final int particleHeight;
	private final int damageRateDelay;
	private final int damageAmount;

	private boolean borderCollapseMessageSent = false;
	private final ServerBossInfo bossInfo;

	public WorldBorderMinigameBehavior(final String name, final BlockPos worldBorderCenter, final String collapseMessage, final long ticksUntilStart,
			final long delayUntilCollapse, final int particleRateDelay, final int particleHeight, final int damageRateDelay, final int damageAmount) {
		this.worldBorderCenter = worldBorderCenter;
		this.collapseMessage = collapseMessage;
		this.ticksUntilStart = ticksUntilStart;
		this.delayUntilCollapse = delayUntilCollapse;
		this.particleRateDelay = particleRateDelay;
		this.particleHeight = particleHeight;
		this.damageRateDelay = damageRateDelay;
		this.damageAmount = damageAmount;

		this.bossInfo = (ServerBossInfo)(new ServerBossInfo(
				new TranslationTextComponent(name),
				BossInfo.Color.WHITE,
				BossInfo.Overlay.PROGRESS))
				.setDarkenSky(false);
	}

	public static <T> WorldBorderMinigameBehavior parse(Dynamic<T> root) {
		final String name = root.get("name").asString("");
		final BlockPos worldBorderCenter = root.get("world_border_center").map(BlockPos::deserialize).orElse(BlockPos.ZERO);
		final String collapseMessage = root.get("collapse_message").asString("");
		final long ticksUntilStart = root.get("ticks_until_start").asLong(0);
		final long delayUntilCollapse = root.get("delay_until_collapse").asLong(0);
		final int particleRateDelay = root.get("particle_rate_delay").asInt(0);
		final int particleHeight = root.get("particle_height").asInt(0);
		final int damageRateDelay = root.get("damage_rate_delay").asInt(0);
		final int damageAmount = root.get("damage_amount").asInt(0);

		return new WorldBorderMinigameBehavior(name, worldBorderCenter, collapseMessage, ticksUntilStart,
				delayUntilCollapse, particleRateDelay, particleHeight, damageRateDelay, damageAmount);
	}

	@Override
	public void onFinish(final IMinigameInstance minigame) {
		borderCollapseMessageSent = false;
		bossInfo.removeAllPlayers();
	}

	@Override
	public void worldUpdate(final IMinigameInstance minigame, World world) {
		tickWorldBorder(world, minigame);
	}

	// TODO: Clean up this mess
	private void tickWorldBorder(final World world, final IMinigameInstance minigame) {
		if (minigame.ticks() >= ticksUntilStart) {
			if (!borderCollapseMessageSent) {
				borderCollapseMessageSent = true;
				minigame.getPlayers().sendMessage(new TranslationTextComponent(collapseMessage).applyTextStyle(TextFormatting.RED));
			}

			long ticksSinceStart = minigame.ticks() - ticksUntilStart;

			boolean isCollapsing = minigame.ticks() >= ticksUntilStart + delayUntilCollapse;
			float borderPercent = 0.01F;
			if (!isCollapsing) {
				borderPercent = 1F - ((float) (ticksSinceStart + 1) / (float) delayUntilCollapse);
			}

			//math safety
			if (borderPercent < 0.01F) {
				borderPercent = 0.01F;
			}

			bossInfo.setPercent(borderPercent);

			float maxRadius = 210;
			float currentRadius = maxRadius * borderPercent;
			float amountPerCircle = 4 * currentRadius;
			float stepAmount = 360F / amountPerCircle;

			int yMin = -10;
			int yStepAmount = 5;

			int randSpawn = 30;

			//particle spawning
			if (world.getGameTime() % particleRateDelay == 0) {

				for (float step = 0; step <= 360; step += stepAmount) {
					for (int yStep = yMin; yStep < particleHeight; yStep += yStepAmount) {
						if (world.rand.nextInt(randSpawn/*yMax - yMin*/) == 0) {
							float xVec = (float) -Math.sin(Math.toRadians(step)) * currentRadius;
							float zVec = (float) Math.cos(Math.toRadians(step)) * currentRadius;
							//world.addParticle(ParticleTypes.EXPLOSION, worldBorderCenter.getX() + xVec, worldBorderCenter.getY() + yStep, worldBorderCenter.getZ() + zVec, 0, 0, 0);
							if (world instanceof ServerWorld) {
								ServerWorld serverWorld = (ServerWorld) world;
								//IParticleData data = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation("heart"));
								serverWorld.spawnParticle(ParticleTypes.EXPLOSION, worldBorderCenter.getX() + xVec, worldBorderCenter.getY() + yStep, worldBorderCenter
										.getZ() + zVec, 1, 0, 0, 0, 1D);
							}
						}

					}
				}

				if (world instanceof ServerWorld) {
					ServerWorld serverWorld = (ServerWorld) world;
					//serverWorld.spawnParticle(ParticleTypes.EXPLOSION, worldBorderCenter.getX(), worldBorderCenter.getY(), worldBorderCenter.getZ(), 1, 0, 0, 0, 1D);
				}
			}

			//player damage
			if (world.getGameTime() % damageRateDelay == 0) {
				for (PlayerEntity playerentity : world.getPlayers()) {
					if (EntityPredicates.NOT_SPECTATING.test(playerentity) && EntityPredicates.IS_LIVING_ALIVE.test(playerentity)) {
						//needs moar predicates
						if (!playerentity.isCreative()) {
							//ignore Y val, only do X Z dist compare
							double d0 = playerentity.getDistanceSq(worldBorderCenter.getX(), playerentity.getPosY(), worldBorderCenter.getZ());
							if (isCollapsing || !(currentRadius < 0.0D || d0 < currentRadius * currentRadius)) {
								//System.out.println("hurt: " + playerentity);
								playerentity.attackEntityFrom(DamageSource.causeExplosionDamage((LivingEntity) null), damageAmount);
								playerentity.addPotionEffect(new EffectInstance(Effects.NAUSEA, 40, 0));
							} else {

							}
						}
					}

					//add boss bar info to everyone in dim if not already registered for it
					if (playerentity instanceof ServerPlayerEntity) {
						if (!bossInfo.getPlayers().contains(playerentity)) {
							bossInfo.addPlayer((ServerPlayerEntity) playerentity);
						}
					}
				}
			}

			//instance.getParticipants()
			//this.actionAllParticipants(instance, (p) -> p.addPotionEffect(new EffectInstance(Effects.SLOW_FALLING, 10 * 20)));

			//world.addParticle(ParticleTypes.EXPLOSION, );
		}
	}
}