package com.lovetropics.minigames.common.core.game;

import net.minecraft.util.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class GameResult<T> {
	private static final GameResult<Unit> OK_UNIT = GameResult.ok(Unit.INSTANCE);

	private final T ok;
	private final Component error;

	private GameResult(T ok, Component error) {
		this.ok = ok;
		this.error = error;
	}

	public static <T> GameResult<T> ok(T ok) {
		return new GameResult<>(ok, null);
	}

	public static GameResult<Unit> ok() {
		return OK_UNIT;
	}

	public static <T> GameResult<T> error(Component error) {
		return new GameResult<>(null, error);
	}

	public static <T> GameResult<T> error(GameException exception) {
		return new GameResult<>(null, exception.getTextMessage());
	}

	public static <T> GameResult<T> fromException(String message, Exception exception) {
		exception.printStackTrace();
		return GameResult.error(new TextComponent(message + ": " + exception.toString()));
	}

	public static <T> CompletableFuture<GameResult<T>> handleException(String message, CompletableFuture<GameResult<T>> future) {
		return future.handle((result, throwable) -> {
			if (throwable instanceof Exception) {
				return GameResult.fromException(message, (Exception) throwable);
			}
			return result;
		});
	}

	public T getOk() {
		return ok;
	}

	public Component getError() {
		return error;
	}

	public boolean isOk() {
		return !isError();
	}

	public boolean isError() {
		return error != null;
	}

	public <U> GameResult<U> map(Function<? super T, ? extends U> function) {
		if (isOk()) {
			return GameResult.ok(function.apply(ok));
		} else {
			return castError();
		}
	}

	public <U> GameResult<U> andThen(Function<? super T, GameResult<U>> function) {
		if (isOk()) {
			return function.apply(ok);
		} else {
			return castError();
		}
	}

	public <U> GameResult<U> mapValue(U value) {
		if (isOk()) {
			return GameResult.ok(value);
		} else {
			return castError();
		}
	}

	public <U> CompletableFuture<GameResult<U>> andThenFuture(Function<T, CompletableFuture<GameResult<U>>> function) {
		if (isOk()) {
			return function.apply(ok);
		} else {
			return CompletableFuture.completedFuture(castError());
		}
	}

	public T orElseGet(Function<Component, T> orElse) {
		if (isOk()) {
			return ok;
		} else {
			return orElse.apply(error);
		}
	}

	@SuppressWarnings("unchecked")
	public <U> GameResult<U> castError() {
		if (isError()) {
			return (GameResult<U>) this;
		} else {
			throw new UnsupportedOperationException("not an error!");
		}
	}
}
