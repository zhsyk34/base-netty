package com.dnake.smart.core.session.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Device;
import io.netty.channel.Channel;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP会话(连接)管理
 */
public final class TCPSessionManager {

	/**
	 * 请求连接(登录后移除)
	 * 一连接即记录,开启线程扫描以关闭超时未登录的连接,故此在登录验证中可不验证登录时间
	 */
	private static final Map<String, Channel> ACCEPT_MAP = new ConcurrentHashMap<>();

	/**
	 * 登录的app连接,key为channel默认id
	 */
	private static final Map<String, Channel> APP_MAP = new ConcurrentHashMap<>(Config.TCP_APP_COUNT_PREDICT);

	/**
	 * 登录的网关连接,key为网关sn号
	 */
	private static final Map<String, Channel> GATEWAY_MAP = new ConcurrentHashMap<>(Config.TCP_GATEWAY_COUNT_PREDICT);

	/**
	 * ----------------------------------------TCP会话事件处理----------------------------------------
	 */

	/**
	 * 初始化连接
	 */
	public static void init(Channel channel) {
		ACCEPT_MAP.put(TCPSessions.id(channel), TCPSessions.active(channel));
	}

	/**
	 * 处理登录请求
	 */
	public static String ready(@NonNull Channel channel, @NonNull Device device, @NonNull String sn, Integer port) {
		return TCPSessions.request(channel, device, sn, port);
	}

	/**
	 * 进行登录码校验
	 */
	public static boolean verify(Channel channel, String code) {
		return TCPSessions.verify(channel, code);
	}

	/**
	 * 完成登录
	 *
	 * @return 分配端口{-1:失败,0:APP,50000+:网关}
	 */
	public static int login(Channel channel) {
		int port = TCPSessions.allocate(channel);
		return TCPSessions.pass(channel, port) ? port : -1;
	}

	public static boolean passed(Channel channel) {
		return TCPSessions.passed(channel);
	}

	/**
	 * 关闭连接
	 */
	public static void close(Channel channel) {
		TCPSessions.close(channel);
	}
}
