package com.dnake.smart.core.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskManager {

	private static final List<Runnable> CALLABLE_LIST = new ArrayList<>();

	private static final List<TimerTask> TIMER_TASKS = new ArrayList<>();

	public static void register(Runnable command) {
		CALLABLE_LIST.add(command);
	}

	public static void register(TimerTask task) {
		TIMER_TASKS.add(task);
	}

	public static void execute() {
		if (!CALLABLE_LIST.isEmpty()) {
			ExecutorService service = Executors.newCachedThreadPool();
			CALLABLE_LIST.forEach(runnable -> service.submit(runnable));
			service.shutdown();
		}

		if (!TIMER_TASKS.isEmpty()) {
			ScheduledExecutorService service = Executors.newScheduledThreadPool(TIMER_TASKS.size());
			TIMER_TASKS.forEach(task -> service.scheduleAtFixedRate(task.command, task.delay, task.period, task.unit));
		}
	}
}
