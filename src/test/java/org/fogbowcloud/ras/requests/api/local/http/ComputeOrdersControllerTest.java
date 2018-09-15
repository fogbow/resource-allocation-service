package org.fogbowcloud.ras.requests.api.local.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fogbowcloud.ras.api.http.ComputeOrdersController;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    // test case: Request a compute creation and test successfully return. Check the response of request
    // and the call of facade for create the compute.
    @Test
    public void testCreateCompute() throws Exception {

        // set up
        Mockito.doReturn(FAKE_ORDER_ID).when(this.facade).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, COMPUTE_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.CREATED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(FAKE_ORDER_ID, result.getResponse().getContentAsString());

        Mockito.verify(this.facade, Mockito.times(1)).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
    }

    // test case: Request a compute creation and test bad request return. Check the response of request
    // and the call of facade for create the compute.
    @Test
    public void testCreateComputeBadRequest() throws Exception {

        //set up
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, COMPUTE_ENDPOINT, getHttpHeaders(), WRONG_BODY);

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.BAD_REQUEST.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals("", result.getResponse().getContentAsString());

        // The request has problems, so the call to Facade is not executed.
        Mockito.verify(this.facade, Mockito.times(0)).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
    }

    // test case: Request a compute creation with an unauthorized user. Check the response of request
    // and the call of facade for create the compute.
    @Test
    public void testCreateComputeUnauthorizedException() throws Exception {

        // set up
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, COMPUTE_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(1)).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
    }

    // test case: Request a compute creation with an unauthenticated user. Check the response of request
    // and the call of facade for create the compute.
    @Test
    public void testCreateComputeUnauthenticatedException() throws Exception {

        // set up
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, COMPUTE_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(1)).createCompute(Mockito.any(ComputeOrder.class), Mockito.anyString());
    }

    // test case: Request the list of all computes status when the facade returns an empty list. 
    // Check the response of request and the call of facade for get the computes status.
    @Test
    public void testGetAllComputeStatusEmptyList() throws Exception {

        // set up
        Mockito.doReturn(new ArrayList<InstanceState>()).when(this.facade).getAllInstancesStatus(Mockito.anyString(), Mockito.any(ResourceType.class));
        String COMPUTE_STATUS_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.STATUS_ENDPOINT;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, COMPUTE_STATUS_ENDPOINT, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        String expectedResult = "[]";
        Assert.assertEquals(expectedResult, result.getResponse().getContentAsString());

        Mockito.verify(this.facade, Mockito.times(1)).getAllInstancesStatus(Mockito.anyString(), Mockito.any(ResourceType.class));

    }

    // test case: Request the list of all computes status when the facade returns a non-empty list. 
    // Check the response of request and the call of facade for get the computes status.
    @Test
    public void testGetAllComputeStatusWhenHasData() throws Exception {

        // set up
        final String FAKE_ID_1 = "fake-Id-1";
        final String FAKE_ID_2 = "fake-Id-2";
        final String FAKE_ID_3 = "fake-Id-3";
        final String FAKE_PROVIDER = "fake-provider";

        InstanceStatus instanceStatus1 = new InstanceStatus(FAKE_ID_1, FAKE_PROVIDER, InstanceState.READY);
        InstanceStatus instanceStatus2 = new InstanceStatus(FAKE_ID_2, FAKE_PROVIDER, InstanceState.READY);
        InstanceStatus instanceStatus3 = new InstanceStatus(FAKE_ID_3, FAKE_PROVIDER, InstanceState.READY);

        List<InstanceStatus> computeStatusList = Arrays.asList(new InstanceStatus[]{instanceStatus1, instanceStatus2, instanceStatus3});
        Mockito.doReturn(computeStatusList).when(this.facade).getAllInstancesStatus(Mockito.anyString(), Mockito.any(ResourceType.class));

        String COMPUTE_STATUS_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.STATUS_ENDPOINT;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, COMPUTE_STATUS_ENDPOINT, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<InstanceStatus>> token = new TypeToken<List<InstanceStatus>>() {
        };
        List<InstanceStatus> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        Assert.assertEquals(3, resultList.size());
        Assert.assertEquals(FAKE_ID_1, resultList.get(0).getInstanceId());
        Assert.assertEquals(FAKE_ID_2, resultList.get(1).getInstanceId());
        Assert.assertEquals(FAKE_ID_3, resultList.get(2).getInstanceId());

        Mockito.verify(this.facade, Mockito.times(1)).getAllInstancesStatus(Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Request a compute by its id with an unauthenticated user. Check the response of request
    // and the call of facade for get the compute.
    @Test
    public void testGetComputeByIdUnauthenticatedException() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).getCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(1)).getCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request a compute by its id with an unauthorized user. Check the response of request
    // and the call of facade for get the compute.
    @Test
    public void testGetComputeByIdUnauthorizedException() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).getCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();


        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(1)).getCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request a compute by its id when the instance is not found. Check the response of request
    // and the call of facade for get the compute.
    @Test
    public void testGetNotFoundComputeById() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new InstanceNotFoundException()).when(this.facade).getCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(1)).getCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request a compute by its id and test successfully return. Check the response of request
    // and the call of facade for get the compute.
    @Test
    public void testGetComputeById() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";

        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        ComputeInstance computeInstance = new ComputeInstance(FAKE_ID);
        Mockito.doReturn(computeInstance).when(this.facade).getCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        ComputeInstance resultComputeInstance = new Gson().fromJson(result.getResponse().getContentAsString(), ComputeInstance.class);
        Assert.assertNotNull(resultComputeInstance);
        Assert.assertEquals(computeInstance.getId(), resultComputeInstance.getId());

        Mockito.verify(this.facade, Mockito.times(1)).getCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Delete a compute by its id and test successfully return. Check the response of request
    // and the call of facade for delete the compute.
    @Test
    public void testDeleteExistingCompute() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doNothing().when(this.facade).deleteCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Delete a compute with unauthenticated user. Check the response of request
    // and the call of facade for delete the compute.
    @Test
    public void testDeleteUnauthenticatedException() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).deleteCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Delete a compute with unauthorized user. Check the response of request
    // and the call of facade for delete the compute.
    @Test
    public void testDeleteUnauthorizedException() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).deleteCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Delete a compute not found. Check the response of request
    // and the call of facade for delete the compute.
    @Test
    public void testDeleteNotFoundCompute() throws Exception {

        // set up
        final String FAKE_ID = "fake-Id-1";
        String computeIdEndpoint = COMPUTE_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new InstanceNotFoundException()).when(this.facade).deleteCompute(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, computeIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteCompute(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user allocation with unauthenticated user. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocationUnauthenticatedException() throws Exception {

        // set up
        final String FAKE_MEMBER_ID = "fake-member-id";
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).getComputeAllocation(Mockito.anyString(), Mockito.anyString());
        final String ALLOCATION_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.ALLOCATION_ENDPOINT;
        final String memberIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_MEMBER_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, memberIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getComputeAllocation(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user allocation with unauthorized user. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocationUnauthorizedException() throws Exception {


        // set up
        final String FAKE_MEMBER_ID = "fake-member-id";
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).getComputeAllocation(Mockito.anyString(), Mockito.anyString());
        final String ALLOCATION_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.ALLOCATION_ENDPOINT;
        final String memberIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_MEMBER_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, memberIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getComputeAllocation(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user allocation and test successfully return. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocation() throws Exception {

        // set up
        final String FAKE_MEMBER_ID = "fake-member-id";
        final int VCPU_TOTAL = 1;
        final int RAM_TOTAL = 1;
        final int INSTANCES_TOTAL = 1;

        ComputeAllocation fakeComputeAllocation = new ComputeAllocation(VCPU_TOTAL, RAM_TOTAL, INSTANCES_TOTAL);

        Mockito.doReturn(fakeComputeAllocation).when(this.facade).getComputeAllocation(Mockito.anyString(), Mockito.anyString());

        final String ALLOCATION_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.ALLOCATION_ENDPOINT;
        final String memberIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_MEMBER_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, memberIdEndpoint, getHttpHeaders(), "");

        // set up
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        ComputeAllocation resultComputeAllocation = new Gson().fromJson(result.getResponse().getContentAsString(), ComputeAllocation.class);

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(fakeComputeAllocation.getInstances(), resultComputeAllocation.getInstances());
        Assert.assertEquals(fakeComputeAllocation.getRam(), resultComputeAllocation.getRam());
        Assert.assertEquals(fakeComputeAllocation.getvCPU(), resultComputeAllocation.getvCPU());

        Mockito.verify(this.facade, Mockito.times(1)).getComputeAllocation(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user quota and test successfully return. Check the response of request
    // and the call of facade for get the user quota.
    @Test
    public void testGetUserQuota() throws Exception {

        // set up
        final String FAKE_MEMBER_ID = "fake-member-id";
        final int VCPU_TOTAL = 1;
        final int RAM_TOTAL = 1;
        final int INSTANCES_TOTAL = 1;

        ComputeAllocation fakeTotalComputeAllocation = new ComputeAllocation(VCPU_TOTAL * 2, RAM_TOTAL * 2, INSTANCES_TOTAL * 2);
        ComputeAllocation fakeUsedComputeAllocation = new ComputeAllocation(VCPU_TOTAL, RAM_TOTAL, INSTANCES_TOTAL);
        ComputeQuota fakeUserQuota = new ComputeQuota(fakeTotalComputeAllocation, fakeUsedComputeAllocation);

        Mockito.doReturn(fakeUserQuota).when(this.facade).getComputeQuota(Mockito.anyString(), Mockito.anyString());

        final String QUOTA_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.QUOTA_ENDPOINT;
        final String memberIdEndpoint = QUOTA_ENDPOINT + "/" + FAKE_MEMBER_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, memberIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        ComputeQuota resultComputeQuota = new Gson().fromJson(result.getResponse().getContentAsString(), ComputeQuota.class);

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Assert.assertEquals(fakeUsedComputeAllocation.getInstances(), resultComputeQuota.getUsedQuota().getInstances());
        Assert.assertEquals(fakeUsedComputeAllocation.getvCPU(), resultComputeQuota.getUsedQuota().getvCPU());
        Assert.assertEquals(fakeUsedComputeAllocation.getRam(), resultComputeQuota.getUsedQuota().getRam());

        Assert.assertEquals(fakeTotalComputeAllocation.getInstances(), resultComputeQuota.getTotalQuota().getInstances());
        Assert.assertEquals(fakeTotalComputeAllocation.getvCPU(), resultComputeQuota.getTotalQuota().getvCPU());
        Assert.assertEquals(fakeTotalComputeAllocation.getRam(), resultComputeQuota.getTotalQuota().getRam());

        Mockito.verify(this.facade, Mockito.times(1)).getComputeQuota(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user quota with unauthenticated user. Check the response of request
    // and the call of facade for get the user quota.
    @Test
    public void testGetUserQuotaUnauthenticatedException() throws Exception {

        // set up
        final String FAKE_MEMBER_ID = "fake-member-id";
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).getComputeQuota(Mockito.anyString(), Mockito.anyString());
        final String QUOTA_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.QUOTA_ENDPOINT;
        final String memberIdEndpoint = QUOTA_ENDPOINT + "/" + FAKE_MEMBER_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, memberIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getComputeQuota(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user quota with unauthorized user. Check the response of request
    // and the call of facade for get the user quota.
    @Test
    public void testGetUserQuotaUnauthorizedException() throws Exception {

        // set up
        final String FAKE_MEMBER_ID = "fake-member-id";
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).getComputeQuota(Mockito.anyString(), Mockito.anyString());
        final String QUOTA_ENDPOINT = COMPUTE_ENDPOINT + "/" + ComputeOrdersController.QUOTA_ENDPOINT;
        final String memberIdEndpoint = QUOTA_ENDPOINT + "/" + FAKE_MEMBER_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, memberIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getComputeQuota(Mockito.anyString(), Mockito.anyString());
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
