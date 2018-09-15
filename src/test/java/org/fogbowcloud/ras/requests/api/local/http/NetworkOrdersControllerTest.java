package org.fogbowcloud.ras.requests.api.local.http;

import com.google.gson.Gson;
import org.fogbowcloud.ras.api.http.NetworkOrdersController;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = NetworkOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class NetworkOrdersControllerTest {

    private static final String CORRECT_BODY =
            "{\"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", \"gateway\":\"gateway\", \"address\":\"address\", \"allocation\":\"dynamic\"}";

    private static final String NETWORK_END_POINT = "/" + NetworkOrdersController.NETWORK_ENDPOINT;

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
    }

    // test case: Create a network instance
    @Test
    public void createdNetworkTest() throws Exception {

        // set up
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        String orderId = "orderId";
        Mockito.doReturn(orderId).when(this.facade).createNetwork(Mockito.any(NetworkOrder.class), Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(NETWORK_END_POINT)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(CORRECT_BODY)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        int expectedStatus = HttpStatus.CREATED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).createNetwork(Mockito.any(NetworkOrder.class), Mockito.anyString());
    }

    // test case: Get a network instance
    @Test
    public void getNetworkTest() throws Exception {

        // set up
        NetworkInstance instance = createNetworkInstance();

        String networkId = instance.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(instance).when(this.facade).getNetwork(Mockito.anyString(), Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();


        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_END_POINT + "/" + networkId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        NetworkInstance resultInstance = new Gson().fromJson(result.getResponse().getContentAsString(), NetworkInstance.class);
        Assert.assertEquals(instance.getId(), resultInstance.getId());
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getNetwork(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Delete an existing network instance
    @Test
    public void deleteNetworkTest() throws Exception {

        // set up
        NetworkOrder networkOrder = createNetworkOrder();

        String networkId = networkOrder.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doNothing().when(this.facade).deleteNetwork(Mockito.anyString(), Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();


        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_END_POINT + "/" + networkId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();


        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteNetwork(Mockito.anyString(), Mockito.anyString());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(NetworkOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }

    private NetworkOrder createNetworkOrder() {
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider", "fake-token", "fake-user", "fake-name");

        NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
        networkOrder.setFederationUserToken(federationUserToken);

        return networkOrder;
    }

    private NetworkInstance createNetworkInstance() {
        String id = "fake-id";
        String label = "fake-label";
        String address = "fake-address";
        String gateway = "fake-gateway";
        String vLan = "fake-vlan";
        return new NetworkInstance(id, InstanceState.READY, label, address, gateway, vLan, NetworkAllocationMode.STATIC, null, null, null);
    }
}
