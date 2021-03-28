package com.lovetropics.minigames.common.minigames.behaviours.instances;

import com.lovetropics.minigames.common.MoreCodecs;
import com.lovetropics.minigames.common.minigames.IMinigameInstance;
import com.lovetropics.minigames.common.minigames.behaviours.IMinigameBehavior;
import com.lovetropics.minigames.common.minigames.behaviours.IPollingMinigameBehavior;
import com.lovetropics.minigames.common.minigames.polling.PollingMinigameInstance;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class CommandInvokeBehavior implements IMinigameBehavior, IPollingMinigameBehavior {
	private static final Logger LOGGER = LogManager.getLogger(CommandInvokeBehavior.class);

	public static final Codec<String> COMMAND_CODEC = Codec.STRING.xmap(
			command -> {
				if (command.startsWith("/")) {
					command = command.substring(1);
				}
				return command;
			},
			Function.identity()
	);

	public static final Codec<Map<String, List<String>>> COMMANDS_CODEC = Codec.unboundedMap(Codec.STRING, MoreCodecs.listOrUnit(COMMAND_CODEC));

	protected final Map<String, List<String>> commands;

	private CommandDispatcher<CommandSource> dispatcher;
	private CommandSource source;

	public CommandInvokeBehavior(Map<String, List<String>> commands) {
		this.commands = commands;
	}

	public void invoke(String key) {
		this.invoke(key, this.source);
	}

	public void invoke(String key, CommandSource source) {
		List<String> commands = this.commands.get(key);
		if (commands == null || commands.isEmpty()) {
			return;
		}

		for (String command : commands) {
			try {
				this.dispatcher.execute(command, source);
			} catch (CommandSyntaxException e) {
				LOGGER.error("Failed to execute command `{}` for {}", command, key, e);
			}
		}
	}

	public CommandSource sourceForEntity(Entity entity) {
		return this.source.withEntity(entity).withPos(entity.getPositionVec());
	}

	@Override
	public void onStartPolling(PollingMinigameInstance minigame) {
		MinecraftServer server = minigame.getServer();
		this.dispatcher = server.getCommandManager().getDispatcher();
		this.source = server.getCommandSource();
	}

	@Override
	public void onConstruct(IMinigameInstance minigame) {
		this.source = minigame.getCommandSource();
	}
}
