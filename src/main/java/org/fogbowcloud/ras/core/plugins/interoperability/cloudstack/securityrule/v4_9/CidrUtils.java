package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CidrUtils {
	private static Pattern VALID_IPV4_PATTERN = null;
	private static Pattern VALID_IPV6_PATTERN = null;
	private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

	static {
		try {
			VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
			VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
		}
	}

	public static boolean isIpv6(String ipAddress) {
		Matcher m1 = CidrUtils.VALID_IPV6_PATTERN.matcher(ipAddress);
		if (m1.matches()) {
			return true;
		}
		return false;
	}
	
	public static boolean isIpv4(String ipAddress) {
		Matcher m1 = CidrUtils.VALID_IPV4_PATTERN.matcher(ipAddress);
		if (m1.matches()) {
			return true;
		}
		return false;
	}	

}
