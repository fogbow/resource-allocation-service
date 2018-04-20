package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestAttendOpenOrdersThread {

	private Thread attendOpenOrdersThread;

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private OrderRegistry orderRegistry;

	private Properties properties;

	@Before
	public void setUp() {
		String localMemberId = "local-member";
		this.properties.setProperty(ConfigurationConstants.XMPP_ID_KEY, localMemberId);

		this.orderRegistry = Mockito.mock(OrderRegistry.class);
		this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
		this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

		this.attendOpenOrdersThread = new AttendOpenOrdersThread(this.localInstanceProvider,
				this.remoteInstanceProvider, this.orderRegistry, localMemberId, this.properties);
	}

	@Test
	public void testAttendLocalOpenOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		Order localOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				imageName, userData);
		
		Mockito.doReturn(localOrder).when(this.orderRegistry).getNextOpenOrder();
	}
}
