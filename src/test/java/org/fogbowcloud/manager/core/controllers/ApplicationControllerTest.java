package org.fogbowcloud.manager.core.controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.PluginHelper;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationControllerTest extends BaseUnitTests {

	private static final Object IDENTITY_URL_KEY = "identity_url";
	private static final Object KEYSTONE_URL = "http://localhost:" + PluginHelper.PORT_ENDPOINT;
	
	private Properties properties;
	private LdapIdentityPlugin ldapIdentityPlugin;
	private ApplicationController applicationController;

	@Before
	public void setUp() throws UnauthorizedException {
		this.properties = new Properties();
        this.properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
        this.setLdapIdentityPlugin();
        AuthenticationService authenticationService = new AuthenticationService(this.ldapIdentityPlugin);
        this.applicationController = Mockito.spy(ApplicationController.getInstance());
        this.applicationController.setAuthenticationController(authenticationService);
	}

	@After
	public void tearDown() {
		super.tearDown();
	}

	@Test
	public void testDeleteComputeOrderSuccessful() throws UnauthorizedException {
		String orderId = this.getComputeOrderCreationId();
		String accessId = "fake-access-id";
		
		this.applicationController.deleteOrder(orderId, accessId, OrderType.COMPUTE);		

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList closedOrdersList = sharedOrderHolders.getClosedOrdersList();
		Order test = closedOrdersList.getNext();
		
		Assert.assertNotNull(test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}
	
	@Test
	public void testThrowUnauthorizedException() throws UnauthorizedException {
		String orderId = "fake-order-id-1";
		String accessId = "fake-access-id!#!fake-user-id!#!fake-user-name";
		Mockito.doThrow(new UnauthorizedException()).when(this.applicationController).deleteOrder(orderId, accessId, OrderType.COMPUTE);
	}

	private void setLdapIdentityPlugin() throws UnauthorizedException {
		Token token = createToken();
		this.ldapIdentityPlugin = Mockito.spy(new LdapIdentityPlugin(this.properties));
		Mockito.doReturn(token).when(ldapIdentityPlugin).getToken(Mockito.anyString());
	}

	private Token createToken() {
		String accessId = "fake-access-id";
		String tokenUserId = "fake-user-id";
		String tokenUserName = "fake-user-name";
		Token.User tokenUser = new Token.User(tokenUserId, tokenUserName);
		Date expirationTime = new Date();
		Map<String, String> attributesMap = new HashMap<>();
		return new Token(accessId, tokenUser, expirationTime, attributesMap);
	}
	
	private String getComputeOrderCreationId() {
		String orderId = null;
		
		Token token = this.createToken();
		
		ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
		computeOrder.setOrderState(OrderState.OPEN);
		computeOrder.setLocalToken(token);
		computeOrder.setFederationToken(token);
		
		orderId = computeOrder.getId();
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(orderId, computeOrder);
		
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(computeOrder);
		
		return orderId;
	}

}
