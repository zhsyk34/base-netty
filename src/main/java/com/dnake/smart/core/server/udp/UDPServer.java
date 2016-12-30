package com.dnake.smart.core.server.udp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * UDP服务器,主要用于接收保存心跳信息以唤醒下线网关登录
 */
public final class UDPServer {

	private static final Lock lock = new ReentrantLock();

	@Getter
	private static Channel channel;

	public static void start() {
		lock.lock();
		if (channel != null) {
			return;
		}

		Bootstrap bootstrap = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			bootstrap.group(group).channel(NioDatagramChannel.class);
			bootstrap.option(ChannelOption.SO_BROADCAST, false);
			bootstrap.handler(new ChannelInitializer<DatagramChannel>() {
				@Override
				protected void initChannel(DatagramChannel ch) throws Exception {
					ch.pipeline().addLast(new UDPCoder()).addLast(new UDPServerHandler());
				}
			});

			channel = bootstrap.bind(Config.UDP_SERVER_PORT).syncUninterruptibly().channel();
			lock.unlock();

			Log.logger(Factory.UDP_EVENT, UDPServer.class.getSimpleName() + " 在端口[" + Config.UDP_SERVER_PORT + "]启动完毕");
			channel.closeFuture().await();
		} catch (Exception e) {
			lock.unlock();
			e.printStackTrace();
		} finally {
			lock.unlock();
			group.shutdownGracefully();
		}
	}
}
