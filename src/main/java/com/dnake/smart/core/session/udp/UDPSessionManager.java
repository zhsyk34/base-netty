package com.dnake.smart.core.session.udp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Result;
import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.message.UDPPusher;
import com.dnake.smart.core.server.udp.UDPServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UDP心跳管理
 */
public final class UDPSessionManager {
	/**
	 * 网关UDP心跳记录,key=sn
	 */
	private static final Map<String, UDPInfo> GATEWAY_MAP = new ConcurrentHashMap<>();

	public static UDPInfo find(String sn) {
		return GATEWAY_MAP.get(sn);
	}

	/**
	 * 缓存接收的网关心跳信息
	 */
	public static void receive(UDPInfo info) {
		GATEWAY_MAP.put(info.getSn(), info);
	}

	/**
	 * 响应网关心跳
	 */
	public static void reply(InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.OK.getName());
		send(target, json);
	}

	public static void awake(String host, int port) {
		awake(new InetSocketAddress(host, port));
	}

	private static void awake(InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.ACTION.getName(), Action.LOGIN_INFORM.getName());
		send(target, json);
	}

	/**
	 * 清理过期的数据
	 */
	public static void monitor() {
		Log.logger(Factory.UDP_EVENT, "当前UDP在线网关数:[" + GATEWAY_MAP.size() + "]");
		Iterator<Map.Entry<String, UDPInfo>> iterator = GATEWAY_MAP.entrySet().iterator();
		while (iterator.hasNext()) {
			UDPInfo info = iterator.next().getValue();
			long createTime = info.getHappen();
			if (!ValidateKit.time(createTime, Config.UDP_HEART_DUE)) {
				iterator.remove();
			}
		}
	}

	/**
	 * @param target 目标地址
	 * @param json   JSON数据
	 */
	private static void send(InetSocketAddress target, JSONObject json) {
		if (UDPServer.getChannel() == null) {
			Log.logger(Factory.UDP_SEND, UDPServer.class.getSimpleName() + " 尚未启动");
			return;
		}
		ByteBuf buf = Unpooled.copiedBuffer(json.toString().getBytes(CharsetUtil.UTF_8));
		UDPServer.getChannel().writeAndFlush(new DatagramPacket(buf, target));
	}

	/**
	 * 推送udp信息至web服务器
	 */
	public static void push() {
		List<UDPInfo> list = new ArrayList<>(GATEWAY_MAP.values());

		final int batch = 10;

		for (int i = 0; i < list.size(); i += batch) {
			JSONObject json = new JSONObject();
			json.put(Key.ACTION.getName(), Action.UDP_SESSION_PUSH.getName());
			json.put(Key.DATA.getName(), list.subList(i, Math.min(i + batch, list.size())));
			UDPPusher.push(json.toString());
		}
	}
}
