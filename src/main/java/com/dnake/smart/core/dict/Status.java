package com.dnake.smart.core.dict;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话状态
 */
@AllArgsConstructor
@Getter
public enum Status {

	CREATED(0, "创建连接"),
	REQUEST(1, "请求登录"),
	VERIFIED(2, "登录验证"),
	ALLOCATED(3, "等待资源(网关登录等待系统分配端口)"),
	PASSED(4, "登录成功(分配端口)"),
	CLOSED(5, "连接关闭");

	private final int step;
	private final String description;

}
