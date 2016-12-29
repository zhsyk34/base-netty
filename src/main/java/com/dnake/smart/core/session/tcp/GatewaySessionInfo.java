package com.dnake.smart.core.session.tcp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 网关会话信息
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "of")
@Getter
final class GatewaySessionInfo {
	private final String sn;
	private final String ip;
	private final int port;
	private final int allot;
	private final long happen;
}