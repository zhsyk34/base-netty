package com.dnake.smart.core.session.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 网关TCP会话信息
 */
@AllArgsConstructor(staticName = "of")
@Getter
@ToString
public final class TCPInfo {
	private final String sn;
	private final String ip;
	private final int port;
	private final int allot;
	private final long happen;
}