package com.lovetropics.minigames.common.core.game.state.statistics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class StatisticsMap {
	private final Map<StatisticKey<?>, Object> values = new Reference2ObjectOpenHashMap<>();

	public <T> void set(StatisticKey<T> key, T value) {
		values.put(key, value);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T get(StatisticKey<T> key) {
		return (T) values.get(key);
	}

	public <T> T getOr(StatisticKey<T> key, T or) {
		T value = get(key);
		return value != null ? value : or;
	}

	public <T> T getOrElse(StatisticKey<T> key, Supplier<T> orElse) {
		T value = get(key);
		return value != null ? value : orElse.get();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T remove(StatisticKey<T> key) {
		return (T) values.remove(key);
	}

	public boolean contains(StatisticKey<?> key) {
		return values.containsKey(key);
	}

	public <T> WithDefault<T> withDefault(StatisticKey<T> key, Supplier<T> defaultSupplier) {
		return new WithDefault<>(key, defaultSupplier);
	}

	public void incrementInt(StatisticKey<Integer> key, int increment) {
		this.withDefault(key, () -> 0).apply(value -> value + increment);
	}

	public JsonElement serialize() {
		JsonObject root = new JsonObject();
		for (Map.Entry<StatisticKey<?>, Object> entry : values.entrySet()) {
			StatisticKey<?> key = entry.getKey();
			root.add(key.getKey(), key.serializeUnchecked(entry.getValue()));
		}
		return root;
	}

	public class WithDefault<T> {
		private final StatisticKey<T> key;
		private final Supplier<T> defaultSupplier;

		WithDefault(StatisticKey<T> key, Supplier<T> defaultSupplier) {
			this.key = key;
			this.defaultSupplier = defaultSupplier;
		}

		public void apply(UnaryOperator<T> operator) {
			T value = get(key);
			if (value == null) {
				value = defaultSupplier.get();
			}

			value = operator.apply(value);
			set(key, value);
		}

		public void accept(Consumer<T> consumer) {
			T value = get(key);
			if (value == null) {
				value = defaultSupplier.get();
				set(key, value);
			}

			consumer.accept(value);
		}
	}
}
