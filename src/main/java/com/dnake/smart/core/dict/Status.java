package com.dnake.smart.core.dict;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话状态
 */
@AllArgsConstructor
@Getter
public enum Status {

	ACTIVE("初始连接"),
	REQUEST("连接请求"),

	/**
	 * 中间态:验证后转为 PASSED
	 */
	VERIFY("连接验证"),
	PASSED("通过验证"),

	/**
	 * 瞬时态:直接关闭并移除
	 */
	CLOSED("连接关闭");

	private final String description;

}
