package com.dnake.smart.core.session.tcp;

import com.dnake.smart.core.config.Config;
import com.dnake.smart.core.dict.Device;
import com.dnake.smart.core.dict.Key;
import com.dnake.smart.core.dict.Status;
import com.dnake.smart.core.kit.CodecKit;
import com.dnake.smart.core.kit.ConvertKit;
import com.dnake.smart.core.kit.RandomKit;
import com.dnake.smart.core.kit.ValidateKit;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.NonNull;

import java.net.InetSocketAddress;

/**
 * TCP会话 信息&&状态 处理工具类
 */
public final class TCPSessions {

	 /* ----------------------------------------连接信息----------------------------------------*/

	private static String ip(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

	private static int port(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getPort();
	}

	/**
	 * 连接的默认id
	 */
	public static String id(@NonNull Channel channel) {
		return channel.id().asLongText();
	}

	 /* ----------------------------------------获取缓存信息----------------------------------------*/

	/**
	 * 获取连接身份
	 */
	public static String sn(@NonNull Channel channel) {
		return SessionKit.sn(channel);
	}

	/**
	 * 获取设备类型
	 */
	public static Device device(@NonNull Channel channel) {
		return SessionKit.type(channel);
	}

	/**
	 * 获取网关所有缓存的会话信息
	 */
	static TCPInfo info(@NonNull Channel channel) {
		if (SessionKit.type(channel) != Device.GATEWAY) {
			return null;
		}
		return TCPInfo.of(SessionKit.sn(channel), ip(channel), port(channel), SessionKit.port(channel), SessionKit.created(channel));
	}

	 /* ----------------------------------------登录过程中会话信息的验证与保存----------------------------------------*/

	/**
	 * 初始化连接
	 */
	static boolean create(@NonNull Channel channel) {
		SessionKit.create(channel);
		SessionKit.status(channel, Status.CREATED);
		return SessionKit.check(channel);
	}

	/**
	 * 登录请求(准备登录)
	 */
	static String request(@NonNull Channel channel, @NonNull Device device, @NonNull String sn, Integer port) {
		if (SessionKit.status(channel) != Status.CREATED) {
			return null;
		}

		SessionKit.type(channel, device);
		SessionKit.sn(channel, sn);
		SessionKit.port(channel, port);

		int group = RandomKit.randomInteger(0, 49);
		int offset = RandomKit.randomInteger(0, 9);
		SessionKit.code(channel, CodecKit.loginVerify(group, offset));

		SessionKit.status(channel, Status.REQUEST);

		if (!SessionKit.check(channel)) {
			return null;
		}

		return CodecKit.loginKey(group, offset);
	}

	/**
	 * 登录验证
	 */
	static boolean verify(@NonNull Channel channel, @NonNull String code) {
		if (SessionKit.status(channel) != Status.REQUEST) {
			return false;
		}
		SessionKit.status(channel, Status.VERIFIED);

		return SessionKit.check(channel) && code.equals(SessionKit.code(channel));

	}

	/**
	 * 网关在通过验证后的进一步处理
	 * 等待UDP端口分配:
	 * {-1: 失败, 0: APP, 50000 +: 网关}
	 */
	static int allocate(@NonNull Channel channel) {
		if (SessionKit.status(channel) != Status.VERIFIED) {
			return -1;
		}
		Integer allocated;
		switch (SessionKit.type(channel)) {
			case APP:
				allocated = 0;
				break;
			case GATEWAY:
				allocated = PortAllocator.allocate(SessionKit.sn(channel), ip(channel), SessionKit.port(channel));
				break;
			default:
				allocated = -1;
		}
		SessionKit.port(channel, allocated);

		SessionKit.status(channel, Status.ALLOCATED);
		return SessionKit.check(channel) ? allocated : -1;
	}

	/**
	 * 完成登录并将信息推送至WEB服务器
	 */
	static boolean pass(@NonNull Channel channel, Integer port) {
		if (SessionKit.status(channel) != Status.ALLOCATED) {
			return false;
		}
		SessionKit.port(channel, port);
		SessionKit.status(channel, Status.PASSED);
		return SessionKit.check(channel);
	}

	/**
	 * 查看是否已登录
	 */
	static boolean passed(@NonNull Channel channel) {
		return SessionKit.status(channel) == Status.PASSED;
	}

	/**
	 * 关闭连接
	 */
	static boolean close(Channel channel) {
		if (channel != null) {
			SessionKit.status(channel, Status.CLOSED);
			channel.close();
		}
		return true;
	}

	/**
	 * 内部工具类
	 */
	private static final class SessionKit {

		//validate for init time
		private static final long MIN_MILLIS = ConvertKit.from(ConvertKit.from(Config.SERVER_START_TIME));

		/**
		 * ----------------------------以下为登录阶段的缓存信息----------------------------
		 */

		//设备类型
		private static final AttributeKey<Device> TYPE = AttributeKey.newInstance(Key.TYPE.getName());
		//网关SN号
		private static final AttributeKey<String> SN = AttributeKey.newInstance(Key.SN.getName());
		//网关请求的UDP端口
		private static final AttributeKey<Integer> PORT = AttributeKey.newInstance(Key.ALLOT.getName());
		//当前连接的登录验证码
		private static final AttributeKey<String> KEYCODE = AttributeKey.newInstance(Key.KEYCODE.getName());
		//连接状态
		private static final AttributeKey<Status> STATUS = AttributeKey.newInstance(Status.class.getSimpleName());
		//连接创建时间
		private static final AttributeKey<Long> ACTIVE = AttributeKey.newInstance(Status.CREATED.name());

		/**
		 * 1.连接时间
		 */
		private static Channel create(@NonNull Channel channel, long millis) {
			if (millis >= MIN_MILLIS) {
				channel.attr(ACTIVE).set(millis);
			}
			return channel;
		}

		static Channel create(@NonNull Channel channel) {
			return create(channel, System.currentTimeMillis());
		}

		static long created(@NonNull Channel channel) {
			Long created = channel.attr(ACTIVE).get();
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
		static Channel port(@NonNull Channel channel, Integer port) {
			if (SessionKit.validate(port)) {
				channel.attr(PORT).set(port);
			}
			return channel;
		}

		static int port(@NonNull Channel channel) {
			Integer port = channel.attr(PORT).get();
			return validate(port) ? port : -1;
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

		/**
		 * 各状态下缓存数据校验
		 */
		static boolean check(@NonNull Channel channel) {
			Status status = status(channel);
			if (status == null) {
				return false;
			}

			long created = created(channel);
			Device device = type(channel);
			String sn = sn(channel);
			String code = code(channel);
			int port = port(channel);

			switch (status) {
				case CREATED:
					return created >= MIN_MILLIS;
				case REQUEST:
				case VERIFIED:
				case ALLOCATED:
				case PASSED:
					return device != null && ValidateKit.notEmpty(sn, code) && check(device, port);
				default:
					return true;
			}
		}

		private static boolean check(@NonNull Device device, Integer port) {
			switch (device) {
				case APP:
					return true;
				case GATEWAY:
					return validate(port);
				default:
					return true;
			}
		}

		private static boolean validate(Integer port) {
			return port != null && port >= Config.TCP_ALLOT_MIN_UDP_PORT;
		}
	}
}
