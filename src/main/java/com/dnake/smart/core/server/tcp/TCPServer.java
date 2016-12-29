package com.dnake.smart.core.server.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;

/**
 * TCP服务器
 */
public final class TCPServer {

	@Getter
	private static volatile boolean started = false;

	public static synchronized void start() {
		if (started) {
			return;
		}

		ServerBootstrap bootstrap = new ServerBootstrap();

		EventLoopGroup mainGroup = new NioEventLoopGroup();
		EventLoopGroup handleGroup = new NioEventLoopGroup();

		bootstrap.group(mainGroup, handleGroup).channel(NioServerSocketChannel.class);

		//setting options
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_BACKLOG, Config.TCP_SERVER_BACKLOG);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Config.TCP_CONNECT_TIMEOUT * 1000);

		//pool
		bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

		//logging
		bootstrap.childHandler(new LoggingHandler(LogLevel.WARN));

		//handler
		bootstrap.childHandler(new TCPInitializer());

		try {
			ChannelFuture future = bootstrap.bind(Config.TCP_SERVER_HOST, Config.TCP_SERVER_PORT).sync();
			Log.logger(Factory.TCP_EVENT, TCPServer.class.getSimpleName() + " 在端口[" + Config.TCP_SERVER_PORT + "]启动完毕");

			started = true;

			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			mainGroup.shutdownGracefully();
			handleGroup.shutdownGracefully();
		}
	}
}
