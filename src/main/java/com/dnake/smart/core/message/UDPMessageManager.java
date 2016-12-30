package com.dnake.smart.core.message;

/**
 * TODO
 */
@Deprecated
public class UDPMessageManager {

	/**
	 * 将网关TCP推送信息推送至web服务器
	 */
	public static void push(String msg) {
		UDPPusher.push(msg);
	}

}
