package org.medcamp.feedback.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Counts recent attempts per key (such as an IP address and camp) in memory. Good enough for one
 * laptop at a camp; the counts reset when the app restarts.
 */
@Component
public class RateLimiter {

	/** Above this many keys, keys with no recent attempts are dropped. */
	private static final int CLEANUP_THRESHOLD = 10_000;
	private static final Duration FORGET_AFTER = Duration.ofHours(1);

	private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
	private final Clock clock;

	public RateLimiter(Clock clock) {
		this.clock = clock;
	}

	/** Records an attempt and returns true, or returns false if {@code limit} was already reached. */
	public boolean tryAcquire(String key, int limit, Duration window) {
		Instant now = clock.instant();
		if (attempts.size() > CLEANUP_THRESHOLD) {
			Instant cutoff = now.minus(FORGET_AFTER);
			attempts.values().removeIf(times -> {
				synchronized (times) {
					return times.isEmpty() || times.peekLast().isBefore(cutoff);
				}
			});
		}
		Deque<Instant> times = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (times) {
			Instant windowStart = now.minus(window);
			while (!times.isEmpty() && !times.peekFirst().isAfter(windowStart)) {
				times.pollFirst();
			}
			if (times.size() >= limit) {
				return false;
			}
			times.addLast(now);
			return true;
		}
	}

}
