package com.dnake.smart.core.server.udp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.dict.Action;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Result;
import com.dnake.smart.core.kit.JsonKit;
import com.dnake.smart.core.kit.ValidateKit;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.udp.UDPInfo;
import com.dnake.smart.core.session.udp.UDPSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

/**
 * UDP服务器处理器,接收网关心跳
 */
final class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private UDPInfo validate(DatagramPacket msg) {
		String content = msg.content().toString(CharsetUtil.UTF_8);

		JSONObject map = JsonKit.map(content);

		Result result = Result.from(map.getString(Key.RESULT.getName()));
		if (result == Result.OK) {
			Log.logger(Factory.UDP_RECEIVE, "网关响应登录唤醒");
			return null;
		}

		Action action = Action.from(map.getString(Key.ACTION.getName()));
		String sn = map.getString(Key.SN.getName());
		String version = map.getString(Key.VERSION.getName());
		if (action != Action.HEART_BEAT || ValidateKit.isEmpty(sn) || ValidateKit.isEmpty(version)) {
			Log.logger(Factory.UDP_RECEIVE, "非法的心跳信息");
			return null;
		}

		return UDPInfo.from(sn, msg.sender(), version);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		UDPInfo info = validate(msg);
		if (info != null) {
			UDPSessionManager.receive(info);
			UDPSessionManager.reply(msg.sender());
		}
	}
}
