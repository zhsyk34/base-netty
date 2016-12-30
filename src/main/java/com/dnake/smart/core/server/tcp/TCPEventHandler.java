package com.dnake.smart.core.server.tcp;

import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.session.tcp.TCPSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 监听TCP连接事件
 */
final class TCPEventHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Log.logger(Factory.TCP_EVENT, ctx.channel().remoteAddress() + " 发起连接");
		TCPSessionManager.init(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Log.logger(Factory.TCP_EVENT, ctx.channel().remoteAddress() + " 关闭连接");
		TCPSessionManager.close(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		Log.logger(Factory.TCP_ERROR, ctx.channel().remoteAddress() + " 发生错误", cause);
		this.channelInactive(ctx);
	}

	@Override
	public boolean isSharable() {
		return true;
	}
}
