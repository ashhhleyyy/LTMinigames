package com.lovetropics.minigames.common.content.biodiversity_blitz.plot;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public final class CurrencyManager implements IGameState {
	public static final GameStateKey<CurrencyManager> KEY = GameStateKey.create("Currency");

	private final IGamePhase game;

	private final Item item;
	private final Predicate<ItemStack> itemPredicate;

	private final Object2IntMap<UUID> trackedValues = new Object2IntOpenHashMap<>();
	private final Object2IntOpenHashMap<UUID> accumulator = new Object2IntOpenHashMap<>();

	public CurrencyManager(IGamePhase game, Item item) {
		this.game = game;
		this.item = item;
		this.itemPredicate = stack -> stack.getItem() == item;

		this.trackedValues.defaultReturnValue(0);
	}

	public void tickTracked() {
		for (ServerPlayerEntity player : game.getParticipants()) {
			if (player.tickCount % 5 == 0) {
				int value = this.get(player);
				this.setTracked(player, value);
			}
		}
	}

	private void setTracked(ServerPlayerEntity player, int value) {
		int lastValue = this.trackedValues.put(player.getUUID(), value);
		if (lastValue != value) {
			this.game.invoker(BbEvents.CURRENCY_CHANGED).onCurrencyChanged(player, value, lastValue);
		}
	}

	private void incrementTracked(ServerPlayerEntity player, int amount) {
		int value = this.trackedValues.getInt(player.getUUID());
		this.setTracked(player, value + amount);
	}

	public int set(ServerPlayerEntity player, int value, boolean accumulate) {
		int oldValue = this.get(player);
		if (value > oldValue) {
			int increment = value - oldValue;
			return oldValue + add(player, increment, accumulate);
		} else if (value < oldValue) {
			int decrement = oldValue - value;
			return oldValue - remove(player, decrement);
		} else {
			return value;
		}
	}

	public int add(ServerPlayerEntity player, int amount, boolean accumulate) {
		int added = this.addToInventory(player, amount);
		this.incrementTracked(player, added);

		// accumulate only when added by currency behavior!
		if (accumulate) {
			this.accumulateCurrency(player, added);
		}

		return added;
	}

	private void accumulateCurrency(ServerPlayerEntity player, int added) {
		int lastValue = this.accumulator.addTo(player.getUUID(), added);
		int newValue = lastValue + added;

		this.game.invoker(BbEvents.CURRENCY_ACCUMULATE).onCurrencyChanged(player, newValue, lastValue);

		this.game.getStatistics().forPlayer(player)
				.set(StatisticKey.POINTS, newValue);
	}

	public int remove(ServerPlayerEntity player, int amount) {
		int removed = this.removeFromInventory(player, amount);
		this.incrementTracked(player, -removed);
		return removed;
	}

	private int addToInventory(ServerPlayerEntity player, int amount) {
		ItemStack stack = new ItemStack(item, amount);
		player.inventory.add(stack);
		sendInventoryUpdate(player);
		return amount - stack.getCount();
	}

	private int removeFromInventory(ServerPlayerEntity player, int amount) {
		int remaining = amount;

		List<Slot> slots = player.containerMenu.slots;
		for (Slot slot : slots) {
			remaining -= this.removeFromSlot(slot, remaining);
			if (remaining <= 0) break;
		}

		remaining -= ItemStackHelper.clearOrCountMatchingItems(player.inventory.getCarried(), itemPredicate, remaining, false);

		sendInventoryUpdate(player);

		return amount - remaining;
	}

	private int removeFromSlot(Slot slot, int amount) {
		ItemStack stack = slot.getItem();
		if (itemPredicate.test(stack)) {
			int removed = Math.min(amount, stack.getCount());
			stack.shrink(removed);
			return removed;
		}

		return 0;
	}

	public int get(ServerPlayerEntity player) {
		int count = 0;

		List<Slot> slots = player.containerMenu.slots;
		for (Slot slot : slots) {
			ItemStack stack = slot.getItem();
			if (itemPredicate.test(stack)) {
				count += stack.getCount();
			}
		}

		ItemStack stack = player.inventory.getCarried();
		if (itemPredicate.test(stack)) {
			count += stack.getCount();
		}

		return count;
	}

	private static void sendInventoryUpdate(ServerPlayerEntity player) {
		player.containerMenu.broadcastChanges();
		player.broadcastCarriedItem();
	}

	public void equalize() {
		int sum = IntStream.of(this.accumulator.values().toIntArray()).sum();
		int count = this.accumulator.keySet().size();
		int newSum = sum / count;

		for (UUID uuid : this.accumulator.keySet()) {
			this.accumulator.put(uuid, newSum);
		}
	}
}
