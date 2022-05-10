package de.npcomplete.throttle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import clojure.lang.IFn;

/**
 * Decrements the used up amount continuously over time.
 * This means there is no fixed time window which resets the amount.
 * Instead, the entire amount can be exhausted in a burst,
 * and then can continue in the throttled manner.
 */
public final class Throttle {
	private final long amount;
	private final long decTime;

	private long start;
	private long used;

	public Throttle(long amount, long time, TimeUnit unit) {
		this.amount = amount;
		decTime = unit.toNanos(time) / amount;
		start = System.nanoTime();
	}

	private synchronized boolean update() {
		var now = System.nanoTime();
		var decrements = (now - start) / decTime;
		used = decrements > used ? 0 : used - decrements;
		start += decrements * decTime;
		if (used >= amount) {
			return false;
		}
		used++;
		return true;
	}

	public Object fetch(IFn fn) {
		while (!update()) {
			LockSupport.parkNanos(decTime);
		}
		return fn.invoke();
	}
}
