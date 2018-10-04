package org.fogbowcloud.ras.requests.api.local.http;

import com.google.gson.Gson;
import org.fogbowcloud.ras.api.http.VolumeOrdersController;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
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

import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = VolumeOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class VolumeOrdersControllerTest {

    private static final String CORRECT_BODY =
            "{\"federationToken\": null, \"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", \"volumeSize\": 1}";
    private static final String VOLUME_END_POINT = "/" + VolumeOrdersController.VOLUME_ENDPOINT;
    private static final String FAKE_FEDERATION_TOKEN_VALUE = "fake-access-id";
    private static final String FAKE_ID = "fake-id";
    private static final String FAKE_NAME = "fake-name";

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
    }

    // test case: When calling the createVolume() method, it must return a ID of the Order and to
    // confirm the HttpStatus as Created.
    @Test
    public void createdVolumeTest() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);

        // exercise
        Mockito.doReturn(order.getId()).when(this.facade)
                .createVolume(Mockito.any(VolumeOrder.class), Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(VOLUME_END_POINT)
                .headers(headers).accept(MediaType.APPLICATION_JSON).content(CORRECT_BODY)
                .contentType(MediaType.APPLICATION_JSON)).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();

        String resultVolumeId = result.getResponse().getContentAsString();

        // verify
        Mockito.verify(this.facade, times(1)).createVolume(Mockito.any(VolumeOrder.class),
                Mockito.anyString());

        Assert.assertEquals(order.getId(), resultVolumeId);
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    // test case: When calling the getVolume() method, it must return a VolumeInstance and to
    // confirm the HttpStatus as Ok.
    @Test
    public void getVolumeTest() throws Exception {

        // set up
        VolumeInstance volumeInstance = createVolumeInstance();

        String volumeId = volumeInstance.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);

        // exercise
        Mockito.doReturn(volumeInstance).when(this.facade).getVolume(Mockito.anyString(),
                Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc
                .perform(MockMvcRequestBuilders.get(VOLUME_END_POINT + "/" + volumeId)
                        .headers(headers).contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        VolumeInstance resultInstance = new Gson()
                .fromJson(result.getResponse().getContentAsString(), VolumeInstance.class);

        // verify
        Mockito.verify(this.facade, times(1)).getVolume(Mockito.anyString(), Mockito.anyString());

        Assert.assertEquals(volumeInstance.getId(), resultInstance.getId());
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    // test case: When calling the deleteVolume() method, it must receive the HttpStatus as Ok.
    @Test
    public void deleteVolumeTest() throws Exception {

        // set up
        VolumeOrder volumeOrder = createVolumeOrder();
        String volumeId = volumeOrder.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);

        // exercise
        Mockito.doNothing().when(this.facade).deleteVolume(Mockito.anyString(),
                Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();

        MvcResult result = this.mockMvc
                .perform(MockMvcRequestBuilders.delete(VOLUME_END_POINT + "/" + volumeId)
                        .headers(headers).contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int expectedStatus = HttpStatus.OK.value();

        // verify
        Mockito.verify(this.facade, times(1)).deleteVolume(Mockito.anyString(), Mockito.anyString());

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(VolumeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY,
                FAKE_FEDERATION_TOKEN_VALUE);

        return headers;
    }

    private VolumeOrder createVolumeOrder() throws InvalidParameterException {
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider", "fake-token-value", FAKE_ID, FAKE_NAME);

        VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
        volumeOrder.setFederationUserToken(federationUserToken);

        return volumeOrder;
    }

    private VolumeInstance createVolumeInstance() {
        return new VolumeInstance(FAKE_ID);
    }

}
