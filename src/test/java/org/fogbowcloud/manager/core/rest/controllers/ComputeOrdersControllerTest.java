package org.fogbowcloud.manager.core.rest.controllers;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.exceptions.OrdersServiceException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.PluginHelper;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.fogbowcloud.manager.core.services.OrdersService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.Assert.*;

public class ComputeOrdersControllerTest {

    private ApplicationController applicationController;
    private IdentityPlugin ldapIdentityPlugin;
    private OrdersService ordersService;
    private Properties properties;

    private final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:" + PluginHelper.PORT_ENDPOINT;
    private final String FAKE_ACCESS_ID = "11111";
    private final String FAKE_LOCAL_TOKEN_ID = "00000";
    private final String ACESS_ID_HEADER = "accessId";
    private final String LOCAL_TOKEN_ID_HEADER = "localTokenId";

    @Before
    public void setUp() throws OrdersServiceException, UnauthorizedException {
        this.properties = new Properties();
        this.properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
        mockLdapIdentityPlugin();
        AuthenticationService authenticationController = new AuthenticationService(ldapIdentityPlugin);
        this.applicationController = ApplicationController.getInstance();
        this.applicationController.setAuthenticationController(authenticationController);
        mockComputeOrdersService();
    }

    @Test
    public void createdComputeTest() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
        //headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String fakeAccessId = "llfdsfdsf";
        String fakeLocalTokenId = "37498327432";
        headers.set(ACESS_ID_HEADER, fakeAccessId);
        headers.set(LOCAL_TOKEN_ID_HEADER, fakeLocalTokenId);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        String url = "http://localhost:" + PluginHelper.PORT_ENDPOINT + "/compute";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
    }

    private void mockLdapIdentityPlugin() throws UnauthorizedException {
        Token token = getFakeToken();
        this.ldapIdentityPlugin = Mockito.spy(new LdapIdentityPlugin(this.properties));
        Mockito.doReturn(token).when(ldapIdentityPlugin).getToken(Mockito.anyString());
    }

    private void mockComputeOrdersService() throws OrdersServiceException {
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