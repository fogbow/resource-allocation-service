package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.PluginHelper;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.fogbowcloud.manager.core.services.OrdersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.junit.Assert;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OrdersServiceTest {

    private ApplicationController applicationController;
    private IdentityPlugin ldapIdentityPlugin;
    private OrdersService ordersService;
    private Properties properties;

    private final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:" + PluginHelper.PORT_ENDPOINT;
    private final String FAKE_ACCESS_ID = "11111";
    private final String FAKE_LOCAL_TOKEN_ID = "00000";

    @Before
    public void setUp() throws OrderManagementException, UnauthorizedException {
        this.properties = new Properties();
        this.properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
        mockLdapIdentityPlugin();
        AuthenticationService authenticationController = new AuthenticationService(ldapIdentityPlugin);
        this.applicationController = ApplicationController.getInstance();
        this.applicationController.setAuthenticationController(authenticationController);
        mockComputeOrdersService();
    }

    @Test
    public void testCreatedComputeOrder() throws UnauthorizedException, OrdersServiceException {
        ComputeOrder computeOrder = new ComputeOrder();
        ordersService.createOrder(computeOrder, FAKE_ACCESS_ID, FAKE_LOCAL_TOKEN_ID);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
    }

    @Test
    public void testCreateComputeOrderBadRequest() {
        ComputeOrder nullComputeOrder = null;
        ordersService.createOrder(nullComputeOrder, FAKE_ACCESS_ID, FAKE_LOCAL_TOKEN_ID);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testCreateComputeOrderUnauthorized() {
        // set a non mocked LdaPIdentityPlugin
        IdentityPlugin mLdapIdentityPlugin = new LdapIdentityPlugin(this.properties);
        AuthenticationService authenticationController = new AuthenticationService(mLdapIdentityPlugin);
        this.applicationController.setAuthenticationController(authenticationController);

        ComputeOrder computeOrder = new ComputeOrder();
        ResponseEntity<Order> response = ordersService.createCompute(computeOrder, FAKE_ACCESS_ID, FAKE_LOCAL_TOKEN_ID);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    private void mockLdapIdentityPlugin() throws UnauthorizedException {
        Token token = getFakeToken();
        this.ldapIdentityPlugin = Mockito.spy(new LdapIdentityPlugin(this.properties));
        Mockito.doReturn(token).when(ldapIdentityPlugin).getToken(Mockito.anyString());
    }

    private void mockComputeOrdersService() throws OrderManagementException {
        ordersService =  Mockito.spy(new OrdersService());
        Mockito.doNothing().when(ordersService).addOrderInActiveOrdersMap(Mockito.any(ComputeOrder.class));
    }

    private Token getFakeToken() {
        String fakeAccessId = "0000";
        String fakeUserId = "userId";
        String fakeUserName = "userName";
        Token.User fakeUser = new Token.User(fakeUserId, fakeUserName);
        Date fakeExpirationTime = new Date();
        Map<String, String> fakeAttributes = new HashMap<>();
        return  new Token(fakeAccessId, fakeUser, fakeExpirationTime, fakeAttributes);
    }

}