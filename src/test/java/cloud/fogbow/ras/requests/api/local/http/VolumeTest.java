package cloud.fogbow.ras.requests.api.local.http;

import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.request.Volume;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import com.google.gson.Gson;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.InvalidParameterException;

import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = Volume.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class VolumeTest {

    private static final String CORRECT_BODY =
            "{\"federationToken\": null, \"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", \"volumeSize\": 1}";
    private static final String VOLUME_END_POINT = "/" + Volume.VOLUME_ENDPOINT;
    private static final String FAKE_FEDERATION_TOKEN_VALUE = "fake-access-id";
    private static final String FAKE_ID = "fake-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String ENDPOINT_SUFFIX = "/cloudName";

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
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
        String expectedResponse = String.format("{\"id\":\"%s\"}", order.getId());

        String resultVolumeId = result.getResponse().getContentAsString();

        // verify
        Mockito.verify(this.facade, times(1)).createVolume(Mockito.any(VolumeOrder.class),
                Mockito.anyString());

        Assert.assertEquals(expectedResponse, resultVolumeId);
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

    // test case: Request the user allocation and test successfully return. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocation() throws Exception {
        // set up
        final String FAKE_PROVIDER_ID = "fake-provider-id";
        final int DISK_TOTAL = 1;

        VolumeAllocation fakeVolumeAllocation = new VolumeAllocation(DISK_TOTAL);

        Mockito.doReturn(fakeVolumeAllocation).when(this.facade).getVolumeAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        final String ALLOCATION_ENDPOINT = VOLUME_END_POINT + "/" + Volume.ALLOCATION_SUFFIX_ENDPOINT;
        final String providerIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_PROVIDER_ID + ENDPOINT_SUFFIX;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, providerIdEndpoint, getHttpHeaders(), "");

        // set up
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        VolumeAllocation resultComputeAllocation = new Gson().fromJson(result.getResponse().getContentAsString(), VolumeAllocation.class);

        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getVolumeAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(fakeVolumeAllocation.getStorage(), resultComputeAllocation.getStorage());
    }

    // test case: Request the user allocation with unauthenticated user. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocationUnauthenticatedException() throws Exception {
        // set up
        final String FAKE_PROVIDER_ID = "fake-provider-id";
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).getVolumeAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        final String ALLOCATION_ENDPOINT = VOLUME_END_POINT + "/" + Volume.ALLOCATION_SUFFIX_ENDPOINT;
        final String providerIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_PROVIDER_ID + ENDPOINT_SUFFIX;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, providerIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getVolumeAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user allocation with unauthorized user. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocationUnauthorizedException() throws Exception {
        // set up
        final String FAKE_PROVIDER_ID = "fake-provider-id";
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).getVolumeAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        final String ALLOCATION_ENDPOINT = VOLUME_END_POINT + "/" + Volume.ALLOCATION_SUFFIX_ENDPOINT;
        final String providerIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_PROVIDER_ID + ENDPOINT_SUFFIX;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, providerIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getVolumeAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    private RequestBuilder createRequestBuilder(HttpMethod method, String urlTemplate, HttpHeaders headers, String body) {
        switch (method) {
            case POST:
                return MockMvcRequestBuilders.post(urlTemplate)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON);
            case GET:
                return MockMvcRequestBuilders.get(urlTemplate)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON);
            case DELETE:
                return MockMvcRequestBuilders.delete(urlTemplate)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON);
            default:
                return null;
        }

    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, FAKE_FEDERATION_TOKEN_VALUE);

        return headers;
    }

    private VolumeOrder createVolumeOrder() throws InvalidParameterException, UnexpectedException {
        SystemUser systemUser = new SystemUser(FAKE_ID, FAKE_NAME, "token-provider"
        );
        VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
        volumeOrder.setSystemUser(systemUser);
        return volumeOrder;
    }

    private VolumeInstance createVolumeInstance() {
        return new VolumeInstance(FAKE_ID);
    }

}
