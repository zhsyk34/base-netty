package com.dnake.smart.core.entry;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.kit.ThreadKit;
import com.dnake.smart.core.log.Factory;
import com.dnake.smart.core.log.Log;
import com.dnake.smart.core.message.TCPMessageManager;
import com.dnake.smart.core.message.UDPPusher;
import com.dnake.smart.core.server.tcp.TCPServer;
import com.dnake.smart.core.server.udp.UDPServer;
import com.dnake.smart.core.session.tcp.PortAllocator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO
 * 程序入口
 */
public class Entry {

	public static void start() {
		//从数据加载网关端口使用数据
		PortAllocator.load();

		ExecutorService service = Executors.newCachedThreadPool();

		//TCP服务器
		service.submit(TCPServer::start);
		while (!TCPServer.isStarted()) {
			Log.logger(Factory.TCP_EVENT, TCPServer.class.getSimpleName() + " 正在启动...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME * 1000);
		}

		//UDP服务器
		service.submit(UDPServer::start);
		while (UDPServer.getChannel() == null) {
			Log.logger(Factory.UDP_EVENT, UDPServer.class.getSimpleName() + " 正在启动...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME * 1000);
		}

		//UDP客户端
		service.submit(UDPPusher::start);
		while (UDPPusher.getChannel() == null) {
			Log.logger(Factory.UDP_EVENT, UDPPusher.class.getSimpleName() + " 正在启动...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME * 1000);
		}

		//TCP消息处理和监控
		service.submit(TCPMessageManager::monitor);
		service.submit(TCPMessageManager::process);

		//TODO
//		ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

		/**
		 * UDP
		 */
		//UDP端口回收
//		executor.scheduleWithFixedDelay(PortAllocator::recycle, Config.SCHEDULE_TASK_DELAY_TIME, Config.UDP_PORT_COLLECTION_SCAN_FREQUENCY, TimeUnit.SECONDS);
		//UDP端口信息定期保存至数据库
//		executor.scheduleAtFixedRate(PortAllocator::persistent, Config.UDP_PORT_SAVE_FREQUENCY, Config.UDP_PORT_SAVE_FREQUENCY, TimeUnit.SECONDS);
		//UDP数据同步到WEB服务器
//		executor.scheduleAtFixedRate(UDPSessionManager::push, Config.SCHEDULE_TASK_DELAY_TIME, Config.SCHEDULE_TASK_DELAY_TIME, TimeUnit.SECONDS);
		//UDP连接监控
//		executor.scheduleWithFixedDelay(UDPSessionManager::monitor, Config.SCHEDULE_TASK_DELAY_TIME, Config.UDP_ONLINE_SCAN_FREQUENCY, TimeUnit.SECONDS);

		/**
		 * TCP
		 */
		//TCP连接监控
//		service.scheduleWithFixedDelay(TCPSessionManager::acceptMonitor, Config.SCHEDULE_TASK_DELAY_TIME, TCP_TIME_OUT_SCAN_FREQUENCY, TimeUnit.SECONDS);
//		service.scheduleWithFixedDelay(TCPSessionManager::appMonitor, Config.SCHEDULE_TASK_DELAY_TIME, TCP_TIME_OUT_SCAN_FREQUENCY, TimeUnit.SECONDS);
//		service.scheduleWithFixedDelay(TCPSessionManager::gatewayMonitor, Config.SCHEDULE_TASK_DELAY_TIME, TCP_TIME_OUT_SCAN_FREQUENCY, TimeUnit.SECONDS);

//		//TCP消息处理和监控
//		executor.scheduleWithFixedDelay(TCPMessageManager::monitor, Config.SCHEDULE_TASK_DELAY_TIME, 3, TimeUnit.SECONDS);
//		executor.scheduleWithFixedDelay(TCPMessageManager::process, Config.SCHEDULE_TASK_DELAY_TIME, 3, TimeUnit.SECONDS);
//		service.scheduleWithFixedDelay(MessageManager::persistent, Config.SCHEDULE_TASK_DELAY_TIME, 5, TimeUnit.SECONDS);
		//TODO:日志处理
	}

	public static void main(String[] args) {
		start();
	}

}
