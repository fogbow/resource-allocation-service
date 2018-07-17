package org.fogbowcloud.manager.requests.api.local.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fogbowcloud.manager.api.http.ComputeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = ComputeOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class ComputeOrdersControllerTest {

    private static final String CORRECT_BODY =
            "{\"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", "
                    + "\"publicKey\":\"pub-key\", \"vCPU\":\"2\", \"memory\":\"1024\", \"disk\":\"20\", "
                    + "\"imageName\":\"ubuntu\"}";

    private static final String WRONG_BODY = "";
    private static final String FAKE_ORDER_ID = "fake-order-id";

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    private static final String COMPUTE_ENDPOINT = "/" + ComputeOrdersController.COMPUTE_ENDPOINT;

    @Before
    public void setUp() throws FogbowManagerException {
        this.facade = spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    @Test
    public void testCreateCompute() throws Exception {

        doReturn(FAKE_ORDER_ID).when(this.facade).createCompute(any(ComputeOrder.class), anyString());

        // Need to make a method to create a body based on parameters, also change the mock above
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, COMPUTE_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
        assertEquals(FAKE_ORDER_ID, result.getResponse().getContentAsString());
    }

    // There's tests missing, that we need to implement, forcing Application Controller to throw
    // different exceptionsOLD.

    @Test
    public void testCreateComputeBadRequest() throws Exception {
        // Need to make a method to create a body based on parameters, also change the mock above
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, COMPUTE_ENDPOINT, getHttpHeaders(), WRONG_BODY);

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.BAD_REQUEST.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
        assertEquals("", result.getResponse().getContentAsString());
    }

    @Test
    public void testGetAllComputesEmptyList() throws Exception {
        List<ComputeInstance> computeInstanceList = new ArrayList<>();
        doReturn(computeInstanceList).when(this.facade).getAllComputes(anyString());

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, COMPUTE_ENDPOINT, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        String expectedResult = "[]";
        
        assertEquals(expectedStatus, result.getResponse().getStatus());
        assertEquals(expectedResult, result.getResponse().getContentAsString());
    }

    @Test
    public void testGetAllComputesWhenHasData() throws Exception {
    	final String FAKE_ID_1 = "fake-Id-1";
    	final String FAKE_ID_2 = "fake-Id-2";
    	final String FAKE_ID_3 = "fake-Id-3";
    	
        ComputeInstance computeInstance1 = new ComputeInstance(FAKE_ID_1);
        ComputeInstance computeInstance2 = new ComputeInstance(FAKE_ID_2);
        ComputeInstance computeInstance3 = new ComputeInstance(FAKE_ID_3);

        List<ComputeInstance> computeInstanceList = Arrays.asList(new ComputeInstance[] {computeInstance1, computeInstance2, computeInstance3});
        doReturn(computeInstanceList).when(this.facade).getAllComputes(anyString());

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, COMPUTE_ENDPOINT, getHttpHeaders(), "");
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
        
        TypeToken<List<ComputeInstance>> token = new TypeToken<List<ComputeInstance>>(){};
        List<ComputeInstance> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        assertTrue(resultList.size() == 3);
        assertEquals(FAKE_ID_1, resultList.get(0).getId());
        assertEquals(FAKE_ID_2, resultList.get(1).getId());
        assertEquals(FAKE_ID_3, resultList.get(2).getId());
    }
    
    @Test
    public void testGetAllComputeStatusEmptyList() throws Exception {
    	
        doReturn(new ArrayList<InstanceState>()).when(this.facade).getAllInstancesStatus(anyString(), any(InstanceType.class));
        
        String COMPUTE_STATUS_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.STATUS_ENDPOINT;

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, COMPUTE_STATUS_ENDPOINT, getHttpHeaders(), "");
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
        
        String expectedResult = "[]";
        assertEquals(expectedResult, result.getResponse().getContentAsString());
       
    }
    
    @Test
    public void testGetAllComputeStatusWhenHasData() throws Exception {
    	final String FAKE_ID_1 = "fake-Id-1";
    	final String FAKE_ID_2 = "fake-Id-2";
    	final String FAKE_ID_3 = "fake-Id-3";
    	
        InstanceStatus instanceStatus1 = new InstanceStatus(FAKE_ID_1, InstanceState.READY);
        InstanceStatus instanceStatus2 = new InstanceStatus(FAKE_ID_2, InstanceState.READY);
        InstanceStatus instanceStatus3 = new InstanceStatus(FAKE_ID_3, InstanceState.READY);
        
        List<InstanceStatus> computeStatusList = Arrays.asList(new InstanceStatus[] {instanceStatus1, instanceStatus2, instanceStatus3});
        doReturn(computeStatusList).when(this.facade).getAllInstancesStatus(anyString(), any(InstanceType.class));
        
        String COMPUTE_STATUS_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.STATUS_ENDPOINT;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, COMPUTE_STATUS_ENDPOINT, getHttpHeaders(), "");
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
        
        TypeToken<List<InstanceStatus>> token = new TypeToken<List<InstanceStatus>>(){};
        List<InstanceStatus> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        assertTrue(resultList.size() == 3);
        assertEquals(FAKE_ID_1, resultList.get(0).getInstanceId());
        assertEquals(FAKE_ID_2, resultList.get(1).getInstanceId());
        assertEquals(FAKE_ID_3, resultList.get(2).getInstanceId());
    }

    @Test
    public void testGetComputeById() throws Exception {
        final String FAKE_ID = "fake-Id-1";
        
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        ComputeInstance computeInstance = new ComputeInstance(FAKE_ID);
        
        doReturn(computeInstance).when(this.facade).getCompute(anyString(), anyString());

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, computeIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        ComputeInstance resultComputeInstance = new Gson().fromJson(result.getResponse().getContentAsString(), ComputeInstance.class);
        assertTrue(resultComputeInstance != null);
        assertEquals(computeInstance.getId(), resultComputeInstance.getId());
    }

    @Test
    public void testGetNotFoundComputeById() throws Exception {
    	final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        doThrow(new InstanceNotFoundException()).when(this.facade).getCompute(anyString(), anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, computeIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.NOT_FOUND.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void deleteExistingCompute() throws Exception {
        final String fakeId = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + fakeId;
        doNothing().when(this.facade).deleteCompute(anyString(), anyString());

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, computeIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
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
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(ComputeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }
}
