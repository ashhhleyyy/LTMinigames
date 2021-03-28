package com.lovetropics.minigames.common.minigames.behaviours.instances;

import com.lovetropics.minigames.common.minigames.ControlCommand;
import com.lovetropics.minigames.common.minigames.IMinigameInstance;
import com.lovetropics.minigames.common.minigames.behaviours.IMinigameBehavior;
import com.lovetropics.minigames.common.minigames.weather.RainType;
import com.lovetropics.minigames.common.minigames.weather.WeatherController;
import com.lovetropics.minigames.common.minigames.weather.WeatherControllerManager;
import com.mojang.serialization.Codec;

public class WeatherControlsBehavior implements IMinigameBehavior {
	public static final Codec<WeatherControlsBehavior> CODEC = Codec.unit(WeatherControlsBehavior::new);

	private WeatherController controller;

	@Override
	public void onConstruct(IMinigameInstance minigame) {
		controller = WeatherControllerManager.forWorld(minigame.getWorld());

		minigame.addControlCommand("start_heatwave", ControlCommand.forAdmins(source -> controller.setHeatwave(true)));
		minigame.addControlCommand("stop_heatwave", ControlCommand.forAdmins(source -> controller.setHeatwave(false)));

		minigame.addControlCommand("start_rain", ControlCommand.forAdmins(source -> controller.setRain(1.0F, RainType.NORMAL)));
		minigame.addControlCommand("stop_rain", ControlCommand.forAdmins(source -> controller.setRain(0.0F, RainType.NORMAL)));

		minigame.addControlCommand("start_acid_rain", ControlCommand.forAdmins(source -> controller.setRain(1.0F, RainType.ACID)));
		minigame.addControlCommand("stop_acid_rain", ControlCommand.forAdmins(source -> controller.setRain(0.0F, RainType.ACID)));

		minigame.addControlCommand("start_wind", ControlCommand.forAdmins(source -> controller.setWind(0.5F)));
		minigame.addControlCommand("stop_wind", ControlCommand.forAdmins(source -> controller.setWind(0.0F)));
	}

	@Override
	public void onFinish(final IMinigameInstance minigame) {
		controller.reset();
	}
}
