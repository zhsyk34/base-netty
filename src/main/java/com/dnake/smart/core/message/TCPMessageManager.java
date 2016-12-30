package com.dnake.smart.core.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.ErrNo;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Result;
import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.tcp.TCPInfo;
import com.dnake.smart.core.session.tcp.TCPSessionManager;
import lombok.NonNull;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP信息管理
 */
public final class TCPMessageManager {

	/**
	 * APP请求消息处理队列
	 */
	private static final Map<String, AppMessagePendQueue> APP_MESSAGE_QUEUE = new ConcurrentHashMap<>();

	/**
	 * 将APP请求添加到消息处理队列
	 */
	public static boolean receive(String sn, AppMessage msg) {
		final AppMessagePendQueue queue;
		synchronized (APP_MESSAGE_QUEUE) {
			if (APP_MESSAGE_QUEUE.containsKey(sn)) {
				queue = APP_MESSAGE_QUEUE.get(sn);
			} else {
				queue = AppMessagePendQueue.instance();
				APP_MESSAGE_QUEUE.put(sn, queue);
			}
		}
		return queue.offer(msg);
	}

	/**
	 * 网关响应app请求后将其从相应的处理队列中移除
	 */
	public static void reply(String sn, String msg) {
		AppMessagePendQueue queue = APP_MESSAGE_QUEUE.get(sn);

		AppMessage message = queue.poll();
		if (message == null) {
			Log.logger(Factory.TCP_EVENT, "由于网关异常导致响应超时,现有的消息队列已被清空");
		} else {
			TCPSessionManager.response(message.getId(), msg);
		}
	}

	/**
	 * 将网关TCP推送信息通过UDP推送至WEB服务器
	 */
	public static void udpPush(@NonNull String sn, @NonNull String msg) {
		JSONObject json = JSON.parseObject(msg);
		json.put(Key.SN.getName(), sn);
		UDPPusher.push(json);
	}

	/**
	 * 将网关上线信息推送至WEB服务器
	 */
	public static void loginPush(@NonNull TCPInfo info) {
		JSONObject json = (JSONObject) JSON.toJSON(info);
		json.put(Key.ACTION.getName(), Action.TCP_LOGIN_PUSH.getName());
		UDPPusher.push(json);
	}

	/**
	 * 将网关下线信息推送至WEB服务器
	 */
	public static void logoutPush(@NonNull String sn) {
		System.err.println(sn + "下线...");
		JSONObject json = new JSONObject();
		json.put(Key.ACTION.getName(), Action.TCP_LOGOUT_PUSH.getName());
		json.put("sn", sn);
		json.put("happen", System.currentTimeMillis());
		UDPPusher.push(json);
	}

	/**
	 * 处理app消息队列
	 */
	@SuppressWarnings("InfiniteLoopStatement")
	public static void process() {
		while (true) {
			AtomicInteger count = new AtomicInteger();

			APP_MESSAGE_QUEUE.forEach((sn, queue) -> {
				count.addAndGet(queue.getQueue().size());
				AppMessage message = queue.peek();
				if (message != null) {
					TCPSessionManager.forward(sn, message.getMessage());
				}
			});

//			if (count.get() > 0) {
//				Log.logger(Factory.TCP_EVENT, "开始处理消息队列,共[" + count.get() + "]条");
//			}
		}
	}

	/**
	 * 网关响应超时则清空当前的请求队列信息并关闭网关连接
	 * 同时提示app
	 */
	@SuppressWarnings("InfiniteLoopStatement")
	public static void monitor() {
		while (true) {
			APP_MESSAGE_QUEUE.forEach((sn, queue) -> {
				if (queue.isSend() && !ValidateKit.time(queue.getTime(), Config.TCP_MESSAGE_HANDLE_TIMEOUT)) {
					TCPSessionManager.close(sn);
					Queue<AppMessage> history = queue.clear();
					if (history != null) {
						Log.logger(Factory.TCP_ERROR, "网关[" + sn + "]响应超时,关闭连接并移除当前所有请求,共[" + history.size() + "]条");
						feedback(history);
					}
				}
			});
		}
	}

	/**
	 * 回馈响应失败
	 *
	 * @param queue 需要回馈的消息队列
	 */
	private static void feedback(Queue<AppMessage> queue) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.NO.getName());
		json.put(Key.ERROR_NO.getName(), ErrNo.TIMEOUT.getCode());
		json.put(Key.ERROR_INFO.getName(), ErrNo.TIMEOUT.getDescription());
		service.submit(() -> queue.forEach(message -> TCPSessionManager.response(message.getId(), json.toString())));
		service.shutdown();
	}

}
