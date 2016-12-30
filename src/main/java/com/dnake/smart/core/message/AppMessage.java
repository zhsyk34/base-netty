package com.dnake.smart.core.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 接收到的app请求信息
 * 保留app请求id以回复
 */
@RequiredArgsConstructor(staticName = "of")
@Getter
@ToString
public final class AppMessage {
	private final String id;
	private final String message;
}