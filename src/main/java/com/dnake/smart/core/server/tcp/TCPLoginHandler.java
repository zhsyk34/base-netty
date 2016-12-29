package com.dnake.smart.core.server.tcp;

import com.alibaba.fastjson.JSONObject;
import com.dnake.smart.core.dict.*;
import com.dnake.smart.core.kit.JsonKit;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.tcp.TCPSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 登录处理
 */
final class TCPLoginHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof String)) {
			return;
		}
		String command = (String) msg;
		Log.logger(Factory.TCP_RECEIVE, "command:\n" + command);

		Channel channel = ctx.channel();

		JSONObject json = JsonKit.map(command);
		Action action = Action.from(json.getString(Key.ACTION.getName()));

		//登录请求
		if (action == Action.LOGIN_REQUEST) {
			ready(channel, json);
			return;
		}

		//登录验证
		Result result = Result.from(json.getString(Key.RESULT.getName()));
		String keyCode = json.getString(Key.KEYCODE.getName());
		if (result == Result.OK && keyCode != null) {
			verify(channel, keyCode);
			return;
		}

		//拦截未登录的连接
		if (TCPSessionManager.passed(channel)) {
			ctx.fireChannelRead(command);
		} else {
			Log.logger(Factory.TCP_EVENT, "尚未登录,拒绝请求");
			TCPSessionManager.close(channel);
		}

	}

	/**
	 * 处理登录请求(准备阶段)
	 */
	private void ready(Channel channel, JSONObject json) {
		JSONObject response = new JSONObject();

		Device device = Device.from(json.getIntValue(Key.TYPE.getName()));
		String sn = json.getString(Key.SN.getName());
		Integer port = json.getInteger(Key.UDP_PORT.getName());

		String code = TCPSessionManager.ready(channel, device, sn, port);
		if (code == null) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERROR_NO.getName(), ErrNo.PARAMETER.getCode());
			channel.writeAndFlush(response);

			Log.logger(Factory.TCP_EVENT, "无效的登录请求,拒绝连接");
			TCPSessionManager.close(channel);
			return;
		}

		response.put(Key.ACTION.getName(), Action.LOGIN_VERIFY.getName());
		response.put(Key.KEY.getName(), code);

		channel.writeAndFlush(response);
	}

	/**
	 * 处理登录请求(验证阶段)
	 *
	 * @param code 客户端请求验证码
	 */
	private void verify(Channel channel, String code) {
		JSONObject response = new JSONObject();

		if (!TCPSessionManager.verify(channel, code)) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERROR_NO.getName(), ErrNo.UNKNOWN.getCode());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		//登录通过
		int allocated = TCPSessionManager.login(channel);

		if (allocated == -1) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERROR_NO.getName(), ErrNo.TIMEOUT.getCode());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		response.put(Key.RESULT.getName(), Result.OK.getName());

		if (allocated > 0) {
			response.put(Key.UDP_PORT.getName(), allocated);//网关登录
		}
		channel.writeAndFlush(response);
	}
}
