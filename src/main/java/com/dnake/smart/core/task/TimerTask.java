package com.dnake.smart.core.task;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
public class TimerTask {
	final Runnable command;
	final long delay;
	final long period;
	final TimeUnit unit;
}