package org.fogbowcloud.manager.requests.api.local.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fogbowcloud.manager.api.http.NetworkOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocation;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
        this.facade = spy(ApplicationFacade.class);
    }

    @Test
    public void createdNetworkTest() throws Exception {
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        String orderId = "orderId"; 
        doReturn(orderId).when(this.facade).createNetwork(any(NetworkOrder.class), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(NETWORK_END_POINT)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(CORRECT_BODY)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.CREATED.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getAllNetworksTest() throws Exception {
        List<NetworkInstance> networkInstances = new ArrayList<>();
        NetworkInstance instance = createNetworkInstance();
        networkInstances.add(instance);

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(networkInstances).when(this.facade).getAllNetworks(anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_END_POINT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        TypeToken<List<NetworkInstance>> token = new TypeToken<List<NetworkInstance>>(){};
        List<NetworkInstance> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());

        assertEquals(1, resultList.size());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getAllNetworksWithEmptyListTest() throws Exception {
        List<NetworkInstance> networkInstances = new ArrayList<>();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(networkInstances).when(this.facade).getAllNetworks(anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_END_POINT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        TypeToken<List<NetworkInstance>> token = new TypeToken<List<NetworkInstance>>(){};
        List<NetworkInstance> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());

        assertEquals(0, resultList.size());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getNetworkTest() throws Exception {
        NetworkInstance instance = createNetworkInstance();

        String networkId = instance.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(instance).when(this.facade).getNetwork(anyString(), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_END_POINT + "/" + networkId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        NetworkInstance resultInstance = new Gson().fromJson(result.getResponse().getContentAsString(), NetworkInstance.class);
        assertEquals(instance.getId(), resultInstance.getId());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void deleteNetworkTest() throws Exception {
        NetworkOrder networkOrder = createNetworkOrder();

        String networkId = networkOrder.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doNothing().when(this.facade).deleteNetwork(anyString(), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_END_POINT + "/" + networkId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.NO_CONTENT.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(NetworkOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }

    private NetworkOrder createNetworkOrder() {
    	FederationUser federationUser = new FederationUser("fake-user", null);

        NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
        networkOrder.setFederationUser(federationUser);

        return networkOrder;
    }

    private NetworkInstance createNetworkInstance() {
        String id = "fake-id";
        String label = "fake-label";
        String address = "fake-address";
        String gateway = "fake-gateway";
        String vLan = "fake-vlan";
        return new NetworkInstance(id, label, InstanceState.READY, address, gateway, vLan, NetworkAllocation.STATIC, null, null, null);
    }
}
