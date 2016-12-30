package com.dnake.smart.core.session.udp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

/**
 * 网关UDP(心跳)信息
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
@Getter
public final class UDPInfo {

	private final String sn;

	private final String ip;
	private final int port;

	private final String version;
	private final long happen;

	public static UDPInfo from(String sn, InetSocketAddress address, String version) {
		String ip = address.getAddress().getHostAddress();
		int port = address.getPort();
		return UDPInfo.of(sn, ip, port, version, System.currentTimeMillis());
	}
}
