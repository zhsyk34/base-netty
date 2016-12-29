package com.dnake.smart.core.dict;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Device {

	GATEWAY(0, "智能网关"),
	APP(1, "手机应用程序");

	private final int type;
	private final String description;

	public static Device from(int type) {
		for (Device device : values()) {
			if (device.getType() == type) {
				return device;
			}
		}
		return null;
	}
}