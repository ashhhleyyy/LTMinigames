package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTideWeatherConfig;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.GamePhase;
import com.lovetropics.minigames.common.core.game.state.GamePhaseState;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.state.weather.WeatherEventType;
import com.mojang.serialization.Codec;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;

import java.util.Random;

public class SurviveTheTideWeatherControlBehavior implements IGameBehavior {
    public static final Codec<SurviveTheTideWeatherControlBehavior> CODEC = SurviveTheTideWeatherConfig.CODEC.xmap(SurviveTheTideWeatherControlBehavior::new, b -> b.config);

    private final SurviveTheTideWeatherConfig config;

    private final Random random = new Random();

    /**
     * instantiate in IslandRoyaleMinigameDefinition
     * - packet sync what is needed
     * - setup instanced overrides on client
     * <p>
     * <p>
     * phases:
     * - 1: semi peacefull, maybe light rain/wind
     * - 2: heavy wind, acid rain
     * - 3: see doc, "an extreme storm encroaches the map slowly towards the centre"
     * --- assuming can also do same things phase 2 does?
     * <p>
     * phases should be in IslandRoyaleMinigameDefinition for use in other places, and this class listens to them
     * <p>
     * rng that can happen:
     * - wind, can operate independently of other rng events
     * <p>
     * rng that only allows 1 of them at a time:
     * - extreme rain
     * - acid rain
     * - heat wave
     * <p>
     * heat wave:
     * - player movement reduced if player pos can see sky
     * <p>
     * rain:
     * - the usual
     * <p>
     * acid rain:
     * - player damage over time
     * - degrade items and armor over time
     * - use normal rain visual too, color changed
     * <p>
     * extreme rain:
     * - fog closes in
     * - pump up weather2 effects
     * - splashing noise while walking
     * - use normal rain visual too
     * <p>
     * - consider design to factor in worn items to negate player effects
     */

    protected GamePhaseState phases;
    protected GameWeatherState weather;

    public SurviveTheTideWeatherControlBehavior(final SurviveTheTideWeatherConfig config) {
        this.config = config;
    }

    @Override
    public void register(IGamePhase game, EventRegistrar events) {
        weather = game.getState().getOrThrow(GameWeatherState.KEY);

        events.listen(GamePhaseEvents.TICK, () -> tick(game));
        events.listen(GamePhaseEvents.STOP, reason -> weather.clear());

        phases = game.getState().getOrNull(GamePhaseState.KEY);

        weather.getController().setConfig(config);
    }

    private void tick(final IGamePhase game) {
        if (phases == null) {
            return;
        }

        weather.tick();

        GamePhase phase = phases.get();

        ServerWorld world = game.getWorld();
        if (world.getGameTime() % 20 == 0) {
            if (weather.getEvent() == null) {
                if (random.nextFloat() <= config.getRainHeavyChance(phase.key)) {
                    heavyRainfallStart(phase);
                } else if (random.nextFloat() <= config.getRainAcidChance(phase.key)) {
                    acidRainStart(phase);
                } else if (random.nextFloat() <= config.getHeatwaveChance(phase.key)) {
                    heatwaveStart(phase);
                } else if (random.nextFloat() <= config.getSandstormChance(phase.key)) {
                    sandstormStart(phase);
                } else if (random.nextFloat() <= config.getSnowstormChance(phase.key)) {
                    snowstormStart(phase);
                }
            }

            weather.setWind(config.getWindSpeed(phase.key));
            if (weather.getEventType() == WeatherEventType.SNOWSTORM || weather.getEventType() == WeatherEventType.SANDSTORM) {
                weather.setWind(0.7F);
            } else {
                weather.setWind(config.getWindSpeed(phase.key));
            }
        }

        IServerWorldInfo worldInfo = (IServerWorldInfo) world.getWorldInfo();
        if (weather.getEventType() == WeatherEventType.HEAVY_RAIN || weather.getEventType() == WeatherEventType.ACID_RAIN) {
            worldInfo.setRaining(true);
            worldInfo.setThundering(true);
        } else {
            worldInfo.setRaining(false);
            worldInfo.setThundering(false);
        }
    }

    // TODO phase names
    private void heavyRainfallStart(GamePhase phase) {
        int time = config.getRainHeavyMinTime() + random.nextInt(config.getRainHeavyExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEventType.HEAVY_RAIN, time);
    }

    private void acidRainStart(GamePhase phase) {
        int time = config.getRainAcidMinTime() + random.nextInt(config.getRainAcidExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEventType.ACID_RAIN, time);
    }

    private void heatwaveStart(GamePhase phase) {
        int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEventType.HEATWAVE, time);
    }

    private void sandstormStart(GamePhase phase) {
        //TODO: more config
        int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEventType.SANDSTORM, time);
    }

    private void snowstormStart(GamePhase phase) {
        //TODO: more config
        int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEventType.SNOWSTORM, time);
    }
}