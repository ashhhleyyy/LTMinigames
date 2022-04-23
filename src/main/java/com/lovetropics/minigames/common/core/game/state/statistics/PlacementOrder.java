package com.lovetropics.minigames.common.core.game.state.statistics;

import com.mojang.serialization.Codec;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nullable;

public enum PlacementOrder implements IStringSerializable {
	MAX("max"),
	MIN("min");

	public static final Codec<PlacementOrder> CODEC = IStringSerializable.fromEnum(PlacementOrder::values, PlacementOrder::byKey);

	private final String key;

	PlacementOrder(String key) {
		this.key = key;
	}

	@Override
	public String getSerializedName() {
		return key;
	}

	@Nullable
	public static PlacementOrder byKey(String key) {
		for (PlacementOrder value : values()) {
			if (value.key.equals(key)) {
				return value;
			}
		}
		return null;
	}
}
