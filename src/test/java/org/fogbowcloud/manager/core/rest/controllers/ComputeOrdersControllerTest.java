package org.fogbowcloud.manager.core.rest.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.controllers.OrdersManagerController;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.PluginHelper;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

// TODO review this tests
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = ComputeOrdersController.class, secure = false)
@PrepareForTest(ApplicationController.class)
public class ComputeOrdersControllerTest {

    public static final String CORRECT_BODY = "{\"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", " +
            "\"publicKey\":\"pub-key\", \"vCPU\":\"12\", \"memory\":\"1024\", \"disk\":\"500\", " +
            "\"imageName\":\"ubuntu\", \"type\":\"compute\"}";
    public static final String COMPUTE_END_POINT = "/compute";
    @Autowired
    private MockMvc mockMvc;

    private ApplicationController applicationController;
    private IdentityPlugin localIdentityPlugin;
    private OrdersManagerController ordersManagerController;
    private Properties properties;

    private final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:" + PluginHelper.PORT_ENDPOINT;
    private final String ACCESS_ID_HEADER = "accessId";
    private final String LOCAL_TOKEN_ID_HEADER = "localTokenId";

    @Before
    public void setUp() throws UnauthenticatedException, Exception {
        this.properties = new Properties();
        this.properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
        this.applicationController = spy(ApplicationController.class);

        mockLocalIdentityPlugin();
        mockAuthentication();
        mockOrdersManagerController();
    }

    @Ignore
    @Test
    public void createdComputeTest() throws Exception {
        HttpHeaders headers = getHttpHeaders();

        // Mocking application controller
        PowerMockito.mockStatic(ApplicationController.class);
        given(ApplicationController.getInstance()).willReturn(this.applicationController);
        doNothing().when(this.applicationController).newOrderRequest(any(Order.class), anyString(), anyString());

        // Need to make a method to create a body based on parameters, also change the mock above
        RequestBuilder requestBuilder = createRequestBuilder(headers, CORRECT_BODY);

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    // There's tests missing, that we need to implement, forcing Application Controller to throw different exceptions.

    @Ignore
    public void wrongBodyToPostComputeTest() throws Exception {

    }


    private RequestBuilder createRequestBuilder(HttpHeaders headers, String body) {
        return MockMvcRequestBuilders
                    .post(COMPUTE_END_POINT)
                    .headers(headers)
                    .accept(MediaType.APPLICATION_JSON).content(body)
                    .contentType(MediaType.APPLICATION_JSON);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeAccessId = "fake-access-id";
        String fakeLocalTokenId = "fake-local-token-id";
        headers.set(ACCESS_ID_HEADER, fakeAccessId);
        headers.set(LOCAL_TOKEN_ID_HEADER, fakeLocalTokenId);
        return headers;
    }

    private void mockLocalIdentityPlugin() throws UnauthorizedException {
        Token token = getFakeToken();
        this.localIdentityPlugin = mock(IdentityPlugin.class);
        doReturn(token).when(this.localIdentityPlugin).getToken(anyString());
    }

    private void mockAuthentication() throws UnauthorizedException, UnauthenticatedException, Exception {
        Token token = getFakeToken();
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        this.applicationController.setAuthenticationController(authenticationService);
        doNothing().when(authenticationService).authenticateAndAuthorize(anyString());
        doReturn(token).when(authenticationService).getFederationToken(anyString());
        doReturn(token).when(authenticationService).getLocalToken(anyString(), anyString());
    }

    private void mockOrdersManagerController() throws OrderManagementException {
        this.ordersManagerController =  spy(new OrdersManagerController());
        doNothing().when(this.ordersManagerController).addOrderInActiveOrdersMap(any(ComputeOrder.class));
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