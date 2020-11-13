package com.lovetropics.minigames.common.minigames.behaviours.instances.donations;

import com.lovetropics.minigames.common.Util;
import com.mojang.datafixers.Dynamic;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.registries.ForgeRegistries;

public class DonationPackageData {
	protected final String packageType;
	protected final ITextComponent messageForPlayer;
	protected final DonationPackageBehavior.PlayerSelect playerSelect;
	protected final ResourceLocation soundOnReceive;

	public DonationPackageData(final String packageType, final ITextComponent messageForPlayer, final DonationPackageBehavior.PlayerSelect playerSelect, final ResourceLocation soundOnReceive) {
		this.packageType = packageType;
		this.messageForPlayer = messageForPlayer;
		this.playerSelect = playerSelect;
		this.soundOnReceive = soundOnReceive;
	}

	public String getPackageType()
	{
		return packageType;
	}

	public ITextComponent getMessageForPlayer()
	{
		return messageForPlayer;
	}

	public DonationPackageBehavior.PlayerSelect getPlayerSelect()
	{
		return playerSelect;
	}

	public ResourceLocation getSoundOnReceive()
	{
		return soundOnReceive;
	}

	public void onReceive(final ServerPlayerEntity player, final String sendingPlayer) {
		if (messageForPlayer != null) {
			final ITextComponent sentByPlayerMessage = new StringTextComponent("Package sent by ").applyTextStyle(TextFormatting.GOLD)
					.appendSibling(new StringTextComponent(sendingPlayer).applyTextStyles(TextFormatting.GREEN, TextFormatting.BOLD));

			player.sendMessage(messageForPlayer);
			player.sendMessage(sentByPlayerMessage);
			player.connection.sendPacket(new SPlaySoundEffectPacket(ForgeRegistries.SOUND_EVENTS.getValue(soundOnReceive), SoundCategory.MASTER, player.getPosX(), player.getPosY(), player.getPosZ(), 0.2f, 1f));
		}
	}

	public static <T> DonationPackageData parse(Dynamic<T> root) {
		final String packageType = root.get("package_type").asString("");
		final ITextComponent messageForPlayer = Util.getTextOrNull(root, "message_for_player");
		final DonationPackageBehavior.PlayerSelect playerSelect = DonationPackageBehavior.PlayerSelect.getFromType(root.get("player_select").asString(DonationPackageBehavior.PlayerSelect.RANDOM.getType())).get();
		final ResourceLocation soundOnReceive = new ResourceLocation(root.get("sound_on_receive").asString(SoundEvents.ITEM_TOTEM_USE.getName().toString()));

		return new DonationPackageData(packageType, messageForPlayer, playerSelect, soundOnReceive);
	}
}