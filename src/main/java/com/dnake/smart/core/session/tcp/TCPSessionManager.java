package com.dnake.smart.core.session.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Device;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
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
		if (TCPSessions.create(channel)) {
			ACCEPT_MAP.put(TCPSessions.id(channel), channel);
		} else {
			close(channel);
		}
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
	 * (分配端口)完成登录并在相应队列中登记
	 */
	public static int pass(Channel channel) {
		if (ACCEPT_MAP.remove(TCPSessions.id(channel)) == null) {
			//正常情况下此连接已被关闭
			Log.logger(Factory.TCP_EVENT, "登录超时(未及时登录,会话已关闭)");
			return -1;
		}

		int port = TCPSessions.allocate(channel);
		switch (port) {
			case -1:
				break;
			case 0:
				APP_MAP.put(TCPSessions.id(channel), channel);
				break;
			default:
				String sn = TCPSessions.sn(channel);
				Channel original = GATEWAY_MAP.remove(sn);
				if (original != null) {
					Log.logger(Factory.TCP_EVENT, "关闭[" + sn + "]已有的连接");
					TCPSessions.close(original);
				}
				GATEWAY_MAP.put(sn, channel);
		}

		return TCPSessions.pass(channel, port) ? port : -1;
	}

	public static boolean passed(Channel channel) {
		return TCPSessions.passed(channel);
	}

	/**
	 * 关闭连接
	 */
	public static boolean close(Channel channel) {
		if (channel == null || !channel.isOpen()) {
			return true;
		}
		Log.logger(Factory.TCP_EVENT, "关闭[" + channel.remoteAddress() + "]连接");
		//先直接关闭channel
		TCPSessions.close(channel);
		String id = TCPSessions.id(channel);

		//在未登录队列中查找
		if (ACCEPT_MAP.remove(id, channel)) {
			return true;
		}

		Device device = TCPSessions.device(channel);

		if (device == null) {
			//尚未登录,应在此前已删除
			Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + "该连接超时未登录已被关闭");
			return false;
		}

		//已进入登录环节
		switch (device) {
			case APP:
				if (APP_MAP.remove(id, channel)) {
					return true;
				} else {
					Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + "客户端关闭出错,在app队列中查找不到该连接(可能在线时长已到被移除)");
					return false;
				}
			case GATEWAY:
				String sn = TCPSessions.sn(channel);
				if (GATEWAY_MAP.remove(sn, channel)) {
					return true;
				} else {
					Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + " 网关关闭出错,在网关队列中查找不到该连接(可能在线时长已到被移除)");
					return false;
				}
			default:
				Log.logger(Factory.TCP_ERROR, "关闭出错,非法的登录数据");
				return false;
		}
	}
}
