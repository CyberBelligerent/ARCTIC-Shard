package com.rahman.arctic.shard.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpUtil {

	private IpUtil() {}

	public static String increment(String ipv4, int delta) {
		if (ipv4 == null || ipv4.isBlank()) throw new IllegalArgumentException("IP is null/blank");
		InetAddress addr;
		try {
			addr = InetAddress.getByName(ipv4.trim());
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IPv4: " + ipv4, e);
		}
		if (!(addr instanceof Inet4Address)) throw new IllegalArgumentException("Not IPv4: " + ipv4);

		byte[] b = addr.getAddress();
		long value = ((b[0] & 0xFFL) << 24) | ((b[1] & 0xFFL) << 16) | ((b[2] & 0xFFL) << 8) | (b[3] & 0xFFL);
		long result = value + delta;
		if (result < 0L || result > 0xFFFFFFFFL)
			throw new IllegalArgumentException("IP arithmetic overflow: " + ipv4 + " + " + delta);

		return ((result >> 24) & 0xFF) + "."
				+ ((result >> 16) & 0xFF) + "."
				+ ((result >> 8) & 0xFF) + "."
				+ (result & 0xFF);
	}

	public static boolean isInCidr(String ipv4, String cidr) {
		if (ipv4 == null || cidr == null) return false;
		int slash = cidr.indexOf('/');
		if (slash < 0) return false;
		String netStr = cidr.substring(0, slash).trim();
		int prefix;
		try {
			prefix = Integer.parseInt(cidr.substring(slash + 1).trim());
		} catch (NumberFormatException e) {
			return false;
		}
		if (prefix < 0 || prefix > 32) return false;

		long net = toLong(netStr);
		long ip = toLong(ipv4);
		long mask = prefix == 0 ? 0L : (~0L << (32 - prefix)) & 0xFFFFFFFFL;
		return (net & mask) == (ip & mask);
	}

	private static long toLong(String ipv4) {
		try {
			byte[] b = InetAddress.getByName(ipv4.trim()).getAddress();
			return ((b[0] & 0xFFL) << 24) | ((b[1] & 0xFFL) << 16) | ((b[2] & 0xFFL) << 8) | (b[3] & 0xFFL);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IPv4: " + ipv4, e);
		}
	}

}
