package com.dnake.smart.core.session.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Device;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Status;
import com.dnake.smart.core.kit.CodecKit;
import com.dnake.smart.core.kit.ConvertKit;
import com.dnake.smart.core.kit.RandomKit;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.NonNull;

import java.net.InetSocketAddress;

/**
 * TCP会话 信息&&状态 处理工具类
 */
final class TCPSessions {

	private static String ip(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

	private static int port(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getPort();
	}

	/**
	 * 连接的默认id
	 */
	static String id(@NonNull Channel channel) {
		return channel.id().asLongText();
	}

	/**
	 * 初始化连接
	 */
	static Channel active(@NonNull Channel channel) {
		SessionKit.status(SessionKit.create(channel), Status.ACTIVE);
		return channel;
	}

	/**
	 * 登录请求(准备登录)
	 */
	static String request(@NonNull Channel channel, @NonNull Device device, @NonNull String sn, Integer port) {
		if (SessionKit.status(channel) != Status.ACTIVE) {
			return null;
		}

		if (device == Device.GATEWAY) {
			if (SessionKit.check(port)) {
				SessionKit.port(channel, port);
			} else {
				return null;
			}
		}

		SessionKit.type(channel, device);
		SessionKit.sn(channel, sn);
		SessionKit.status(channel, Status.REQUEST);

		int group = RandomKit.randomInteger(0, 49);
		int offset = RandomKit.randomInteger(0, 9);

		SessionKit.code(channel, CodecKit.loginVerify(group, offset));

		return CodecKit.loginKey(group, offset);
	}

	/**
	 * 登录验证
	 */
	static boolean verify(@NonNull Channel channel, @NonNull String code) {
		if (SessionKit.status(channel) != Status.REQUEST) {
			return false;
		}
		SessionKit.status(channel, Status.VERIFY);
		return code.equals(SessionKit.code(channel));
	}

	/**
	 * 网关在通过验证后的进一步处理
	 * 等待UDP端口分配
	 */
	static int allocate(@NonNull Channel channel) {
		if (SessionKit.status(channel) != Status.VERIFY) {
			return -1;
		}
		if (SessionKit.type(channel) != Device.GATEWAY) {
			return 0;
		}
		int allocated = PortAllocator.allocate(SessionKit.sn(channel), ip(channel), SessionKit.port(channel));
		SessionKit.port(channel, allocated);
		return allocated;
	}

	/**
	 * 完成登录
	 */
	static boolean pass(@NonNull Channel channel, Integer port) {
		if (SessionKit.status(channel) != Status.VERIFY) {
			return false;
		}

		Device device = SessionKit.type(channel);
		if (device == Device.GATEWAY) {
			if (SessionKit.check(port)) {
				SessionKit.port(channel, port);
			} else {
				return false;
			}
		}
		SessionKit.status(channel, Status.PASSED);
		return true;
	}

	/**
	 * 查看是否已登录
	 */
	static boolean passed(@NonNull Channel channel) {
		return SessionKit.status(channel) == Status.PASSED;
	}

	/**
	 * 释放资源
	 */
	static void close(Channel channel) {
		if (channel != null) {
			SessionKit.status(channel, Status.CLOSED);
			channel.close();
		}
	}

	/**
	 * 获取网关会话信息
	 */
	static GatewaySessionInfo info(@NonNull Channel channel) {
		if (SessionKit.type(channel) != Device.GATEWAY) {
			return null;
		}
		return GatewaySessionInfo.of(SessionKit.sn(channel), ip(channel), port(channel), SessionKit.port(channel), SessionKit.created(channel));
	}

	/**
	 * 内部工具类
	 */
	private static final class SessionKit {

		//validate for create time
		private static final long MIN_MILLIS = ConvertKit.from(ConvertKit.from(Config.SERVER_START_TIME));

		/**
		 * ----------------------------以下为登录阶段的缓存信息----------------------------
		 */

		//设备类型
		private static final AttributeKey<Device> TYPE = AttributeKey.newInstance(Key.TYPE.getName());
		//网关SN号
		private static final AttributeKey<String> SN = AttributeKey.newInstance(Key.SN.getName());
		//网关请求的UDP端口
		private static final AttributeKey<Integer> PORT = AttributeKey.newInstance(Key.UDP_PORT.getName());
		//当前连接的登录验证码
		private static final AttributeKey<String> KEYCODE = AttributeKey.newInstance(Key.KEYCODE.getName());
		//连接状态
		private static final AttributeKey<Status> STATUS = AttributeKey.newInstance(Status.class.getSimpleName());
		//连接创建时间
		private static final AttributeKey<Long> CREATED = AttributeKey.newInstance(Status.ACTIVE.name());

		/**
		 * 1.连接时间
		 */
		private static Channel create(@NonNull Channel channel, long millis) {
			if (millis >= MIN_MILLIS) {
				channel.attr(CREATED).set(millis);
			}
			return channel;
		}

		static Channel create(@NonNull Channel channel) {
			return create(channel, System.currentTimeMillis());
		}

		static long created(@NonNull Channel channel) {
			Long created = channel.attr(CREATED).get();
			return created == null || created < MIN_MILLIS ? -1 : created;
		}

		/**
		 * 2.缓存登录类型
		 */
		static Channel type(@NonNull Channel channel, @NonNull Device device) {
			channel.attr(TYPE).set(device);
			return channel;
		}

		static Device type(@NonNull Channel channel) {
			return channel.attr(TYPE).get();
		}

		/**
		 * 3.缓存(网关或APP请求的目标网关)SN序列号
		 */
		static Channel sn(@NonNull Channel channel, @NonNull String sn) {
			channel.attr(SN).set(sn);
			return channel;
		}

		private static String sn(@NonNull Channel channel) {
			return channel.attr(SN).get();
		}

		/**
		 * 3-1.缓存网关申请的UDP通讯端口号
		 */
		static Channel port(@NonNull Channel channel, int port) {
			if (port >= Config.TCP_ALLOT_MIN_UDP_PORT) {
				channel.attr(PORT).set(port);
			}
			return channel;
		}

		static int port(@NonNull Channel channel) {
			Integer port = channel.attr(PORT).get();
			return port == null || port < Config.TCP_ALLOT_MIN_UDP_PORT ? -1 : port;
		}

		/**
		 * 4.缓存本次登录请求的正确验证码
		 */
		static Channel code(@NonNull Channel channel, @NonNull String code) {
			channel.attr(KEYCODE).set(code);
			return channel;
		}

		static String code(@NonNull Channel channel) {
			return channel.attr(KEYCODE).get();
		}

		/**
		 * 5.缓存登录结果用于过滤非法连接请求
		 */
		static Channel status(@NonNull Channel channel, @NonNull Status status) {
			channel.attr(STATUS).set(status);
			return channel;
		}

		static Status status(@NonNull Channel channel) {
			return channel.attr(STATUS).get();
		}

		static boolean check(Integer port) {
			return port != null && port >= Config.TCP_ALLOT_MIN_UDP_PORT;
		}
	}
}
