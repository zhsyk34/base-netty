package com.dnake.smart.core.server.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.kit.ThreadKit;
import com.dnake.smart.core.session.tcp.TCPSessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TCPServerTest {

	public static void main(String[] args) {
		//TCP服务器
		ExecutorService tcpService = Executors.newSingleThreadExecutor();
		tcpService.submit(TCPServer::start);
		while (!TCPServer.isStarted()) {
			System.out.println(TCPServer.class.getSimpleName() + " 正在启动...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME * 1000);
		}
		tcpService.shutdown();

		ScheduledExecutorService service = Executors.newScheduledThreadPool(8);
		service.scheduleWithFixedDelay(TCPSessionManager::monitor, 1, 5, TimeUnit.SECONDS);
	}
}