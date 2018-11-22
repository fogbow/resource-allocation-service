package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.junit.Assert;
import org.junit.Test;

public class CIRDUtilsTest {

	// test case: is ipv4
	@Test
	public void testIsIpv4() {
		// setup
		String ipAddress = "0.0.0.0";
		// exercise and verify		
		Assert.assertTrue(CIRDUtils.isIpv4(ipAddress));
	}
	
	// test case: is not ipv4
	@Test
	public void testIsNotIpv4() {
		// setup		
		String ipAddress = "0.0a";
		// exercise and verify		
		Assert.assertFalse(CIRDUtils.isIpv4(ipAddress));
	}	
	
	// test case: is ipv6
	@Test
	public void testIsIpv6() {
		// setup		
		String ipAddress = "FE80:0000:0000:0000:0202:B3FF:FE1E:8329";
		// exercise and verify		
		Assert.assertTrue(CIRDUtils.isIpv6(ipAddress));
	}	
	
	// test case: is not ipv6
	@Test
	public void testIsNotIpv6() {
		// setup
		String ipAddress = "FE801230:0000:0000:02653FF:FE1E:8329";
		// exercise and verify		
		Assert.assertFalse(CIRDUtils.isIpv6(ipAddress));
	}		
	
}
