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
import java.util.Map;

import org.fogbowcloud.manager.api.local.http.VolumeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.junit.Before;
import org.junit.Ignore;
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
@WebMvcTest(value = VolumeOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class VolumeOrdersControllerTest {

    public static final String CORRECT_BODY =
            "{\"federationToken\": null, \"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", \"volumeSize\": 1, \"type\":\"volume\"}";

    public static final String VOLUME_END_POINT = "/volume";

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() throws UnauthorizedException, OrderManagementException {
        this.facade = spy(ApplicationFacade.class);
    }

    @Ignore
    @Test
    public void createdVolumeTest() throws Exception {
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doNothing().when(this.facade).createVolume(any(VolumeOrder.class), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(VOLUME_END_POINT)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(CORRECT_BODY)
                .contentType(MediaType.APPLICATION_JSON)).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Ignore
    @Test
    public void getAllVolumesTest() throws Exception {
        String federationTokenValue = "federationTokenValue";

        Map<String, Order> activeOrdersMap = SharedOrderHolders.getInstance().getActiveOrdersMap();
        List<VolumeOrder> volumeOrders = new ArrayList<VolumeOrder>();

        for (Order order : activeOrdersMap.values()) {
            volumeOrders.add((VolumeOrder) order);
        }

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(volumeOrders).when(this.facade).getAllVolumes(federationTokenValue);

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(VOLUME_END_POINT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Ignore
    @Test
    public void getVolumeTest() throws Exception {
        VolumeOrder volumeOrder = createVolumeOrder();

        String volumeId = volumeOrder.getId();
        String federationTokenValue = "federationTokenValue";

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(volumeOrder).when(this.facade).getVolume(volumeId, federationTokenValue);

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(VOLUME_END_POINT)
                .param("volumeId", volumeId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Ignore
    @Test
    public void deleteVolumeTest() throws Exception {
        VolumeOrder volumeOrder = createVolumeOrder();

        String volumeId = volumeOrder.getId();
        String federationTokenValue = "federationTokenValue";

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doNothing().when(this.facade).deleteVolume(volumeId, federationTokenValue);

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(VOLUME_END_POINT)
                .param("volumeId", volumeId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }

    private VolumeOrder createVolumeOrder() {
    	FederationUser federationUser = new FederationUser(-1l, null);

        VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
        volumeOrder.setFederationUser(federationUser);

        String orderId = volumeOrder.getId();

        Map<String, Order> activeOrdersMap = SharedOrderHolders.getInstance().getActiveOrdersMap();
        activeOrdersMap.put(orderId, volumeOrder);

        ChainedList openOrdersList = SharedOrderHolders.getInstance().getOpenOrdersList();
        openOrdersList.addItem(volumeOrder);

        return volumeOrder;
    }
}
