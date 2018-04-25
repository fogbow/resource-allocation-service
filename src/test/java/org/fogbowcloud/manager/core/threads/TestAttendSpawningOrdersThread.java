package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestAttendSpawningOrdersThread {

	private AttendSpawningOrdersThread attendSpawningOrdersThread;
	
	private Properties properties;
	
	@Before
	public void setUp() {
		this.attendSpawningOrdersThread = Mockito.spy(new AttendSpawningOrdersThread());
	}
	
	@Test
	public void testProcessSpawningOrder() {
		
	}
	
	private Order createOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		Order localOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				imageName, userData);
		return localOrder;
	}
	
}
