package com.lovetropics.minigames.common.core.diguise;

import com.lovetropics.minigames.Constants;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Constants.MODID)
public final class PlayerDisguiseBehavior {
	private static final UUID ATTRIBUTE_MODIFIER_UUID = UUID.randomUUID();

	@SubscribeEvent
	public static void onSetEntitySize(EntityEvent.Size event) {
		Entity entity = event.getEntity();

		if (entity instanceof Player player) {
			PlayerDisguise disguise = PlayerDisguise.get(player).orElse(null);
			if (disguise != null) {
				DisguiseType disguiseType = disguise.getDisguiseType();
				Entity disguiseEntity = disguise.getDisguiseEntity();
				if (disguiseType != null && disguiseEntity != null) {
					Pose pose = event.getPose();
					EntityDimensions size = disguiseEntity.getDimensions(pose);

					event.setNewSize(size.scale(disguiseType.scale));
					event.setNewEyeHeight(disguiseEntity.getEyeHeightAccess(pose, size) * disguiseType.scale);
				}
			}
		}
	}

	public static void applyAttributes(Player player, LivingEntity disguise) {
		AttributeMap playerAttributes = player.getAttributes();
		AttributeMap disguiseAttributes = disguise.getAttributes();

		for (Attribute attribute : Registry.ATTRIBUTE) {
			if (!disguiseAttributes.hasAttribute(attribute) || !playerAttributes.hasAttribute(attribute)) {
				continue;
			}

			AttributeInstance playerInstance = playerAttributes.getInstance(attribute);
			AttributeInstance disguiseInstance = disguiseAttributes.getInstance(attribute);

			AttributeModifier modifier = createModifier(playerInstance, disguiseInstance);
			if (modifier != null) {
				playerInstance.addTransientModifier(modifier);
			}
		}
	}

	public static void clearAttributes(Player player) {
		AttributeMap attributes = player.getAttributes();
		for (Attribute attribute : Registry.ATTRIBUTE) {
			AttributeInstance instance = attributes.getInstance(attribute);
			if (instance != null) {
				instance.removeModifier(ATTRIBUTE_MODIFIER_UUID);
			}
		}
	}

	@Nullable
	private static AttributeModifier createModifier(AttributeInstance player, AttributeInstance disguise) {
		double playerValue = player.getBaseValue();
		double disguiseValue = disguise.getBaseValue();

		// non-player movement speeds get effectively squared
		if (disguise.getAttribute() == Attributes.MOVEMENT_SPEED) {
			disguiseValue *= disguiseValue;
		}

		double value = disguiseValue - playerValue;
		if (Math.abs(value) <= 0.001) {
			return null;
		}

		return new AttributeModifier(
				ATTRIBUTE_MODIFIER_UUID, "disguise",
				value,
				AttributeModifier.Operation.ADDITION
		);
	}
}
