package com.dnake.smart.core.server.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

final class TCPInitializer extends ChannelInitializer<SocketChannel> {
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new TCPEventHandler());
		pipeline.addLast(new TCPDecoder());
		pipeline.addLast(new TCPEncoder());
		pipeline.addLast(new TCPLoginHandler());
		pipeline.addLast(new TCPServerHandler());
	}
}
