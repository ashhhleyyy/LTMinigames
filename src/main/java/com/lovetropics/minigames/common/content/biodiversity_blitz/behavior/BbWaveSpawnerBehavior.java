package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public final class BbWaveSpawnerBehavior implements IGameBehavior {
	public static final Codec<BbWaveSpawnerBehavior> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
				Codec.LONG.fieldOf("interval_seconds").forGetter(c -> c.intervalTicks / 20),
				Codec.LONG.fieldOf("warn_seconds").forGetter(c -> c.warnTicks / 20),
				SizeCurve.CODEC.fieldOf("size_curve").forGetter(c -> c.sizeCurve),
				MoreCodecs.object2Float(MoreCodecs.DIFFICULTY).fieldOf("difficulty_factors").forGetter(c -> c.difficultyFactors),
				MoreCodecs.TEXT.optionalFieldOf("first_message", TextComponent.EMPTY).forGetter(c -> c.firstMessage)
		).apply(instance, BbWaveSpawnerBehavior::new);
	});

	private final long intervalTicks;
	private final long warnTicks;
	private final SizeCurve sizeCurve;

	private final Object2FloatMap<Difficulty> difficultyFactors;
	
	private final Component firstMessage;

	private IGamePhase game;
	private PlotsState plots;

	private int sentWaves = 0;
	private Map<UUID, List<WaveTracker>> waveTrackers = new HashMap<>();
	private ServerBossEvent waveCharging;

	public BbWaveSpawnerBehavior(long intervalSeconds, long warnSeconds, SizeCurve sizeCurve, Object2FloatMap<Difficulty> difficultyFactors, Component firstMessage) {
		this.intervalTicks = intervalSeconds * 20;
		this.warnTicks = warnSeconds * 20;
		this.sizeCurve = sizeCurve;

		this.difficultyFactors = difficultyFactors;
		this.difficultyFactors.defaultReturnValue(1.0F);
		
		this.firstMessage = firstMessage;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		this.plots = game.getState().getOrThrow(PlotsState.KEY);

		events.listen(BbEvents.ASSIGN_PLOT, this::addPlayer);
		events.listen(GamePlayerEvents.REMOVE, this::removePlayer);

		events.listen(GamePhaseEvents.TICK, this::tick);
		events.listen(GamePhaseEvents.STOP, reason -> {
			waveTrackers.values().stream()
					.flatMap(List::stream)
					.forEach(WaveTracker::close);
			cleanupBossBar(waveCharging);
		});

		this.waveCharging = new ServerBossEvent(new TextComponent("Wave Incoming!"), BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
		this.waveCharging.setProgress(0.0F);
		this.waveCharging.setVisible(false);
	}

	private void addPlayer(ServerPlayer player, Plot plot) {
		this.waveCharging.addPlayer(player);
	}

	private void removePlayer(ServerPlayer player) {
		this.waveCharging.removePlayer(player);
		List<WaveTracker> waves = this.waveTrackers.remove(player.getUUID());
		if (waves != null) {
			for (WaveTracker wave : waves) {
				wave.close();
			}
		}
	}

	private void tick() {
		ServerLevel world = game.getWorld();
		Random random = world.getRandom();
		long ticks = game.ticks();

		long timeTilNextWave = ticks % intervalTicks;

		if (timeTilNextWave == intervalTicks - warnTicks) {
			game.getParticipants().sendMessage(BiodiversityBlitzTexts.waveWarning(), true);
		}

		if (timeTilNextWave > intervalTicks - 40) {
			this.waveCharging.setProgress(1.0F - (intervalTicks - timeTilNextWave) / 40.0F);
			this.waveCharging.setVisible(true);
		}

		if (timeTilNextWave == 0) {
			this.waveCharging.setVisible(false);
			for (ServerPlayer player : game.getParticipants()) {
				Plot plot = plots.getPlotFor(player);
				if (plot == null) continue;

				this.spawnWave(world, random, player, plot, sentWaves);
			}

			this.sentWaves++;
		}

		this.waveTrackers.forEach((pid, waves) -> {
			Iterator<WaveTracker> iterator = waves.iterator();
			while (iterator.hasNext()) {
				WaveTracker wave = iterator.next();
				if (this.tickWave(wave)) {
					wave.close();
					iterator.remove();
				}
			}
		});
	}

	private boolean tickWave(WaveTracker wave) {
		if (wave.entities.removeIf(e -> !e.isAlive())) {
			wave.bar.setProgress((float) wave.entities.size() / wave.waveSize);
		}
		return wave.entities.isEmpty();
	}

	private void cleanupBossBar(ServerBossEvent bar) {
		bar.removeAllPlayers();
	}

	private void spawnWave(ServerLevel world, Random random, ServerPlayer player, Plot plot, int waveIndex) {
		if (waveIndex == 0 && firstMessage != TextComponent.EMPTY) {
			player.displayClientMessage(firstMessage, false);
		}

		Difficulty difficulty = world.getDifficulty();
		float difficultyFactor = difficultyFactors.getFloat(difficulty);

		double fractionalCount = sizeCurve.apply(waveIndex, difficultyFactor);
		int count = Mth.floor(fractionalCount);
		if (random.nextDouble() > fractionalCount - count) {
			count++;
		}

		int currencyIncr = plot.nextCurrencyIncrement;
		if (currencyIncr < 5) {
			count = random.nextInt(2) + 1;
		} else if (currencyIncr < 10) {
			count = (int) Mth.lerp(currencyIncr / 10.0, 2, count);
		}

		Set<Entity> entities = BbMobSpawner.spawnWaveEntities(world, random, plot, count, waveIndex, BbMobSpawner::selectEntityForWave);
		ServerBossEvent bossBar = this.createWaveBar(player, waveIndex, count, entities);

		WaveTracker wave = new WaveTracker(bossBar, entities);
		waveTrackers.computeIfAbsent(player.getUUID(), $ -> new ArrayList<>()).add(wave);
	}

	private ServerBossEvent createWaveBar(ServerPlayer player, int waveIndex, int count, Set<Entity> entities) {
		ServerBossEvent bossBar = new ServerBossEvent(new TextComponent("Wave " + (waveIndex + 1)), BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
		bossBar.setProgress((float) entities.size() / count);
		bossBar.setColor(BossBarColor.GREEN);
		bossBar.addPlayer(player);

		return bossBar;
	}

	static final class WaveTracker {
		final ServerBossEvent bar;
		final Set<Entity> entities;
		final int waveSize;

		WaveTracker(ServerBossEvent bar, Set<Entity> entities) {
			this.bar = bar;
			this.entities = entities;
			this.waveSize = entities.size();
		}

		void close() {
			bar.removeAllPlayers();
		}
	}

	static final class SizeCurve {
		public static final Codec<SizeCurve> CODEC = RecordCodecBuilder.create(instance -> {
			return instance.group(
					Codec.DOUBLE.fieldOf("lower").forGetter(c -> c.lower),
					Codec.DOUBLE.fieldOf("upper").forGetter(c -> c.upper),
					Codec.DOUBLE.fieldOf("base").forGetter(c -> c.base),
					Codec.DOUBLE.fieldOf("scale").forGetter(c -> c.scale)
			).apply(instance, SizeCurve::new);
		});

		final double lower;
		final double upper;
		final double base;
		final double scale;

		SizeCurve(double lower, double upper, double base, double scale) {
			this.lower = lower;
			this.upper = upper;
			this.base = base;
			this.scale = scale;
		}

		double apply(int index, float difficulty) {
			double lower = this.lower * difficulty;
			double range = this.upper - lower;
			double base = this.base * difficulty;
			double scale = this.scale;
			return lower + range * (1.0 - Math.pow(base, -index * scale / range));
		}
	}
}
