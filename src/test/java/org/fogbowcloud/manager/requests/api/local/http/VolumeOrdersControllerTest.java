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
import org.fogbowcloud.manager.api.http.VolumeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
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
@WebMvcTest(value = VolumeOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class VolumeOrdersControllerTest {

    private static final String CORRECT_BODY =
            "{\"federationToken\": null, \"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", \"volumeSize\": 1}";

    private static final String VOLUME_END_POINT = "/" + VolumeOrdersController.VOLUME_ENDPOINT;

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = spy(ApplicationFacade.class);
    }

    @Test
    public void createdVolumeTest() throws Exception {
        VolumeOrder order = createVolumeOrder();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(order.getId()).when(this.facade).createVolume(any(VolumeOrder.class), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(VOLUME_END_POINT)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(CORRECT_BODY)
                .contentType(MediaType.APPLICATION_JSON)).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();

        String resultVolumeId = result.getResponse().getContentAsString();

        assertEquals(order.getId(), resultVolumeId);
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getAllVolumesTest() throws Exception {
        List<VolumeInstance> volumeInstances = new ArrayList<>();
        VolumeInstance instance = createVolumeInstance();
        volumeInstances.add(instance);

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(volumeInstances).when(this.facade).getAllVolumes(anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(VOLUME_END_POINT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        TypeToken<List<VolumeInstance>> token = new TypeToken<List<VolumeInstance>>(){};
        List<VolumeInstance> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());

        assertEquals(1, resultList.size());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getAllVolumesWithEmptyListTest() throws Exception {
        List<VolumeInstance> volumeInstances = new ArrayList<>();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(volumeInstances).when(this.facade).getAllVolumes(anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(VOLUME_END_POINT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        TypeToken<List<VolumeInstance>> token = new TypeToken<List<VolumeInstance>>(){};
        List<VolumeInstance> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());

        assertEquals(0, resultList.size());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getVolumeTest() throws Exception {
        VolumeInstance volumeInstance = createVolumeInstance();

        String volumeId = volumeInstance.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doReturn(volumeInstance).when(this.facade).getVolume(anyString(), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(VOLUME_END_POINT + "/" + volumeId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        VolumeInstance resultInstance = new Gson().fromJson(result.getResponse().getContentAsString(), VolumeInstance.class);
        assertEquals(volumeInstance.getId(), resultInstance.getId());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void deleteVolumeTest() throws Exception {
        VolumeOrder volumeOrder = createVolumeOrder();
        String volumeId = volumeOrder.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doNothing().when(this.facade).deleteVolume(anyString(), anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(VOLUME_END_POINT + "/" + volumeId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(VolumeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }

    private VolumeOrder createVolumeOrder() {
    	FederationUser federationUser = new FederationUser("fake-user", null);

        VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
        volumeOrder.setFederationUser(federationUser);

        return volumeOrder;
    }

    private VolumeInstance createVolumeInstance() {
        String id = "fake-id";
        return new VolumeInstance(id);
    }

}
