package com.dnake.smart.core.message;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.kit.ValidateKit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 等待网关处理的app消息队列
 */
@NoArgsConstructor(staticName = "instance")
@Getter
@Setter
final class AppMessagePendQueue {
	private final BlockingQueue<AppMessage> queue = new LinkedBlockingQueue<>();
	private volatile boolean send = false;
	private volatile long time = -1;

	/**
	 * 重置队列状态
	 */
	private synchronized AppMessagePendQueue reset() {
		this.send = false;
		this.time = -1;
		return this;
	}

	/**
	 * 当队列数据被处理时开启警戒状态以进行监测
	 */
	private synchronized AppMessagePendQueue guard() {
		this.send = true;
		this.time = System.currentTimeMillis();
		return this;
	}

	/**
	 * 添加数据
	 */
	boolean offer(AppMessage message) {
		return message != null && queue.offer(message);
	}

	/**
	 * 处理队首元素,先查看其是否正被处理
	 * 如是则不进行任何操作,否则取出并进入警戒状态
	 */
	synchronized AppMessage peek() {
		if (send) {
			return null;
		}
		AppMessage message = queue.peek();
		if (message != null) {
			this.guard();
		}
		return message;
	}

	/**
	 * 移除已处理完的数据并重置状态
	 */
	synchronized AppMessage poll() {
		AppMessage message = queue.poll();
		if (message != null) {
			this.reset();
		}
		return message;
	}

	/**
	 * 清空队列并返回当前队列中所有元素的副本
	 */
	synchronized Queue<AppMessage> clear() {
		if (send && !ValidateKit.time(time, Config.TCP_MESSAGE_HANDLE_TIMEOUT)) {
			BlockingQueue<AppMessage> copy = new LinkedBlockingQueue<>(queue);
			queue.clear();
			this.reset();
			return copy;
		}
		return null;
	}
}
