package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.client.toast.NotificationStyle;
import com.lovetropics.minigames.client.toast.NotificationIcon;
import com.lovetropics.minigames.client.toast.ShowNotificationToastMessage;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTideWeatherConfig;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.GamePhase;
import com.lovetropics.minigames.common.core.game.state.GamePhaseState;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.weather.WeatherEvent;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import com.lovetropics.minigames.common.core.network.LoveTropicsNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;
import java.util.function.Supplier;

public class SurviveTheTideWeatherControlBehavior implements IGameBehavior {
    public static final Codec<SurviveTheTideWeatherControlBehavior> CODEC = SurviveTheTideWeatherConfig.CODEC.xmap(SurviveTheTideWeatherControlBehavior::new, b -> b.config);

    private final SurviveTheTideWeatherConfig config;

    private final Random random = new Random();

    /*
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

    private static final Component TITLE = new TextComponent("WEATHER REPORT: \n").withStyle(ChatFormatting.BOLD);

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
    }

    private void tick(final IGamePhase game) {
        if (phases == null) {
            return;
        }

        GamePhase phase = phases.get();

        ServerLevel world = game.getWorld();
        if (world.getGameTime() % 20 == 0) {
            if (weather.getEvent() == null && weather.canStartWeatherEvent()) {
                if (random.nextFloat() <= config.getRainHeavyChance(phase.key())) {
                    heavyRainfallStart(phase);
                } else if (random.nextFloat() <= config.getRainAcidChance(phase.key())) {
                    acidRainStart(game, phase);
                } else if (random.nextFloat() <= config.getHailChance(phase.key())) {
                    hailStart(game, phase);
                } else if (random.nextFloat() <= config.getHeatwaveChance(phase.key())) {
                    heatwaveStart(game, phase);
                } else if (random.nextFloat() <= config.getSandstormChance(phase.key())) {
                    sandstormStart(game, phase);
                } else if (random.nextFloat() <= config.getSnowstormChance(phase.key())) {
                    snowstormStart(game, phase);
                }
            }

            weather.setWind(config.getWindSpeed(phase.key()));
            if (weather.getEventType() == WeatherEventType.SNOWSTORM || weather.getEventType() == WeatherEventType.SANDSTORM) {
                weather.setWind(0.7F);
            } else {
                weather.setWind(config.getWindSpeed(phase.key()));
            }
        }
    }

    // TODO phase names
    private void heavyRainfallStart(GamePhase phase) {
        int time = config.getRainHeavyMinTime() + random.nextInt(config.getRainHeavyExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEvent.heavyRain(time));
    }

    private void acidRainStart(IGamePhase game, GamePhase phase) {
        int time = config.getRainAcidMinTime() + random.nextInt(config.getRainAcidExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEvent.acidRain(time));

        broadcastNotification(game,
                new TextComponent("Acid Rain is falling!\n")
                        .append("Find shelter, or make sure to carry an ")
                        .append(umbrellaName()),
                createNotificationStyle(SurviveTheTide.ACID_REPELLENT_UMBRELLA)
        );
    }

    private void hailStart(IGamePhase game, GamePhase phase) {
        int time = config.getRainHeavyMinTime() + random.nextInt(config.getRainHeavyExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEvent.hail(time));

        broadcastNotification(game,
                new TextComponent("Hail is falling!\n")
                        .append("Find shelter, or make sure to carry an ")
                        .append(umbrellaName()),
                createNotificationStyle(SurviveTheTide.ACID_REPELLENT_UMBRELLA)
        );
    }

    private void heatwaveStart(IGamePhase game, GamePhase phase) {
        int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEvent.heatwave(time));

        broadcastNotification(game,
                new TextComponent("A Heat Wave is passing!\n")
                        .append("Stay inside, or make sure to equip")
                        .append(sunscreenName()),
                createNotificationStyle(SurviveTheTide.SUPER_SUNSCREEN)
        );
    }

    private void sandstormStart(IGamePhase game, GamePhase phase) {
        //TODO: more config
        int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEvent.sandstorm(time, config.getSandstormBuildupTickRate(), config.getSandstormMaxStackable()));

        broadcastNotification(game,
                new TextComponent("A Sandstorm is passing!\n")
                        .append("Find shelter!"),
                createNotificationStyle(() -> Blocks.SAND)
        );
    }

    private void snowstormStart(IGamePhase game, GamePhase phase) {
        //TODO: more config
        int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
        if (phase.is("phase4")) {
            time /= 2;
        }
        weather.setEvent(WeatherEvent.snowstorm(time, config.getSnowstormBuildupTickRate(), config.getSnowstormMaxStackable()));

        broadcastNotification(game,
                new TextComponent("A Snowstorm is passing!\n")
                        .append("Find shelter!"),
                createNotificationStyle(() -> Blocks.SNOW_BLOCK)
        );
    }

    private static void broadcastNotification(IGamePhase game, Component message, NotificationStyle style) {
        ShowNotificationToastMessage packet = new ShowNotificationToastMessage(new TextComponent("").append(TITLE).append(message), style);
        game.getAllPlayers().sendPacket(LoveTropicsNetwork.CHANNEL, packet);
        game.getParticipants().playSound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1.0f, 1.0f);
    }

    private static Component umbrellaName() {
        return new TranslatableComponent(SurviveTheTide.ACID_REPELLENT_UMBRELLA.get().getDescriptionId());
    }

    private static Component sunscreenName() {
        return new TranslatableComponent(SurviveTheTide.SUPER_SUNSCREEN.get().getDescriptionId());
    }

    private static NotificationStyle createNotificationStyle(final Supplier<? extends ItemLike> item) {
        return new NotificationStyle(
                NotificationIcon.item(new ItemStack(item.get())),
                NotificationStyle.Sentiment.NEGATIVE,
                NotificationStyle.Color.DARK,
                5 * 1000
        );
    }
}
