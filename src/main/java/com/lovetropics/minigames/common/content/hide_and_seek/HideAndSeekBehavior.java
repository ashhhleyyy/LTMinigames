package com.lovetropics.minigames.common.content.hide_and_seek;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguise;
import com.lovetropics.minigames.common.core.diguise.ServerPlayerDisguises;
import com.lovetropics.minigames.common.core.dimension.DimensionUtils;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.PlayerSnapshot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

// TODO: clean up and split up
public final class HideAndSeekBehavior implements IGameBehavior {
	public static final Codec<HideAndSeekBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("spawn").forGetter(c -> c.spawnRegionKey),
			Codec.INT.fieldOf("initial_hide_seconds").forGetter(c -> c.initialHideSeconds),
			DisguiseType.CODEC.listOf().fieldOf("disguises").forGetter(c -> c.disguises),
			Creature.CODEC.listOf().fieldOf("creatures").forGetter(c -> c.creatures)
	).apply(i, HideAndSeekBehavior::new));

	private final String spawnRegionKey;
	private final int initialHideSeconds;
	private final List<DisguiseType> disguises;
	private final List<Creature> creatures;

	private IGamePhase game;
	private TeamState teams;
	private GameTeam hiders;
	private GameTeam seekers;

	private BlockBox spawnRegion;

	public HideAndSeekBehavior(String spawnRegionKey, int initialHideSeconds, List<DisguiseType> disguises, List<Creature> creatures) {
		this.spawnRegionKey = spawnRegionKey;
		this.initialHideSeconds = initialHideSeconds;
		this.disguises = disguises;
		this.creatures = creatures;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		teams = game.getState().getOrThrow(TeamState.KEY);

		if (disguises.isEmpty()) {
			throw new GameException(new TextComponent("No possible disguises!"));
		}

		hiders = teams.getTeamByKey("hiders");
		seekers = teams.getTeamByKey("seekers");

		if (hiders == null || seekers == null) {
			throw new GameException(new TextComponent("Missing hiders or seekers team!"));
		}

		spawnRegion = game.getMapRegions().getOrThrow(spawnRegionKey);

		events.listen(GamePhaseEvents.START, this::start);

		events.listen(GamePlayerEvents.SPAWN, (player, role) -> this.spawnPlayer(player));
		events.listen(GamePlayerEvents.SET_ROLE, this::onPlayerSetRole);
		events.listen(GamePlayerEvents.ATTACK, this::onPlayerAttack);
		events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
	}

	private void start() {
		int hideTicks = initialHideSeconds * 20;

		PlayerSet seekers = teams.getPlayersForTeam(this.seekers.key());
		PlayerSet hiders = teams.getPlayersForTeam(this.hiders.key());

		seekers.addPotionEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hideTicks, 255, true, false));
		seekers.addPotionEffect(new MobEffectInstance(MobEffects.BLINDNESS, hideTicks, 255, true, false));

		seekers.sendMessage(new TextComponent("You will be let out to catch the hiders in " + initialHideSeconds + " seconds!"));
		hiders.sendMessage(new TextComponent("You have " + initialHideSeconds + " seconds to hide from the seekers!"));

		for (ServerPlayer player : game.getParticipants()) {
			if (teams.isOnTeam(player, this.hiders.key())) {
				setHider(player);
			} else {
				setSeeker(player);
			}
		}
	}

	private void onPlayerSetRole(ServerPlayer player, PlayerRole role, PlayerRole lastRole) {
		if (lastRole == PlayerRole.PARTICIPANT) {
			this.removeParticipant(player);
		}
	}

	private void spawnPlayer(ServerPlayer player) {
		BlockPos spawnPos = spawnRegion.sample(player.getRandom());
		DimensionUtils.teleportPlayerNoPortal(player, game.getDimension(), spawnPos);
	}

	private InteractionResult onPlayerAttack(ServerPlayer player, Entity target) {
		if (teams.isOnTeam(player, seekers.key()) && player.getMainHandItem().getItem() == HideAndSeek.NET.get()) {
			if (target instanceof ServerPlayer && teams.isOnTeam((ServerPlayer) target, hiders.key())) {
				return InteractionResult.PASS;
			}
		}
		return InteractionResult.FAIL;
	}

	private InteractionResult onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		if (team == hiders.key()) {
			this.onHiderCaptured(player);
		} else if (team == seekers.key()) {
			this.onSeekerDied(player);
		}
		return InteractionResult.FAIL;
	}

	private void removeParticipant(ServerPlayer player) {
		if (teams.isOnTeam(player, hiders.key())) {
			this.removeHider(player);
		}
	}

	private void setHider(ServerPlayer player) {
		DisguiseType disguise = nextDisguiseType(player.level.random);
		ServerPlayerDisguises.set(player, disguise);
	}

	private void setSeeker(ServerPlayer player) {
		PlayerSnapshot.clearPlayer(player);
		player.getInventory().add(new ItemStack(HideAndSeek.NET.get()));
	}

	private DisguiseType nextDisguiseType(Random random) {
		DisguiseType type = disguises.get(random.nextInt(disguises.size()));
		CompoundTag nbt = type.nbt != null ? type.nbt.copy() : new CompoundTag();
		nbt.putBoolean("CustomNameVisible", false);
		return DisguiseType.create(type.type, 1.0f, nbt, type.applyAttributes);
	}

	private void removeHider(ServerPlayer player) {
		PlayerDisguise.get(player).ifPresent(PlayerDisguise::clearDisguise);
	}

	private void onHiderCaptured(ServerPlayer player) {
		teams.addPlayerTo(player, seekers.key());

		this.spawnPlayer(player);
		this.setSeeker(player);
	}

	private void onSeekerDied(ServerPlayer player) {
		this.spawnPlayer(player);
	}

	public static final class Creature {
		public static final Codec<Creature> CODEC = RecordCodecBuilder.create(instance -> {
			return instance.group(
					ForgeRegistries.ENTITIES.getCodec().fieldOf("entity").forGetter(c -> c.entity),
					MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).fieldOf("region").forGetter(c -> c.regionKeys)
			).apply(instance, Creature::new);
		});

		final EntityType<?> entity;
		final String[] regionKeys;

		Creature(EntityType<?> entity, String[] regionKeys) {
			this.entity = entity;
			this.regionKeys = regionKeys;
		}
	}
}
