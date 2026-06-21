package com.voluble.titanMC.mines.reset;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class MineResetQueue {

	private final Map<String, MineResetTask> active = new LinkedHashMap<>();
	private final ArrayDeque<String> order = new ArrayDeque<>();
	private final LongSupplier nanoTime;

	public MineResetQueue() {
		this(System::nanoTime);
	}

	MineResetQueue(LongSupplier nanoTime) {
		this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
	}

	public boolean contains(String name) {
		return active.containsKey(name);
	}

	public void replace(MineResetTask task) {
		Objects.requireNonNull(task, "task");
		cancel(task.name());
		active.put(task.name(), task);
		order.addLast(task.name());
	}

	public void cancel(String name) {
		MineResetTask removed = active.remove(name);
		if (removed != null) removed.cancel();
		order.remove(name);
	}

	public void clear() {
		for (MineResetTask task : active.values()) task.cancel();
		active.clear();
		order.clear();
	}

	public int size() {
		return active.size();
	}

	public void processTick(long budgetNanos, Consumer<String> completed) {
		if (budgetNanos <= 0L) throw new IllegalArgumentException("budgetNanos must be positive");
		Objects.requireNonNull(completed, "completed");
		long deadline = nanoTime.getAsLong() + budgetNanos;
		int available = order.size();
		for (int visit = 0; visit < available; visit++) {
			String name = order.pollFirst();
			if (name == null) return;
			MineResetTask task = active.get(name);
			if (task == null) continue;
			if (visit > 0 && nanoTime.getAsLong() >= deadline) {
				order.addFirst(name);
				return;
			}
			MineResetWork work = task.process(task.maxBlocksPerSlice(), deadline);
			if (work.finished()) {
				active.remove(name);
				completed.accept(name);
			} else {
				order.addLast(name);
			}
		}
	}
}
