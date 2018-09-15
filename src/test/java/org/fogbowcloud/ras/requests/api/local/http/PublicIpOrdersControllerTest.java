package org.fogbowcloud.ras.requests.api.local.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fogbowcloud.ras.api.http.AttachmentOrdersController;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
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

import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = AttachmentOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class PublicIpOrdersControllerTest {
    private final String ATTACHMENT_ENDPOINT =
            "/".concat(AttachmentOrdersController.ATTACHMENT_ENDPOINT);

    private final String CORRECT_BODY =
            "{"
                    + "\"computeOrderId\": \"b8852ff6-ce00-45aa-898d-ddaffb5c6173\""
                    + "}";

    private final String BODY_WITH_EMPTY_PROPERTIES =
            "{"
                    + "\"source\": \"b8852ff6-ce00-45aa-898d-ddaffb5c6173\","
                    + "\"target\": \"596f93c7-06a1-4621-8c9d-5330a089eafe\","
                    + "\"device\": \"/dev/sdd\""
                    + "}";

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowRasException {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    // test case: Request an attachment creation and test successfully return. 
    // Check if the request response is compatible with the value produced by facade.
    @Test
    public void testCreateAttachment() throws Exception {

        // set up
        String orderId = "fake-id";
        Mockito.doReturn(orderId)
                .when(this.facade)
                .createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(
                        HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.CREATED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(orderId, result.getResponse().getContentAsString());

        Mockito.verify(this.facade, Mockito.times(1))
                .createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());
    }

    // test case: Request an attachment creation without request body and test fail return
    // Check if the correct http status is being sent in the request response
    @Test
    public void testPostAttachmentEmptyBody() throws Exception {
        // set up
        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.POST,
                        ATTACHMENT_ENDPOINT,
                        getHttpHeaders(),
                        "");


        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.BAD_REQUEST.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(0))
                .createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());
    }

    // test case: Request an attachment creation with request body without properties and test fail return
    // Check if the correct http status is being sent in the request response
    @Test
    public void testPostAttachmentEmptyJson() throws Exception {
        // set up
        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), "{}");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1))
                .createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());
    }

    // test case: Request an attachment creation with request body without property values and test fail return
    // Check if the correct http status is being sent in the request response
    @Test
    public void testPostAttachmentBodyWithoutProperties() throws Exception {
        // set up
        RequestBuilder requestBuilder =
                createRequestBuilder(
                        HttpMethod.POST,
                        ATTACHMENT_ENDPOINT,
                        getHttpHeaders(),
                        BODY_WITH_EMPTY_PROPERTIES);

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1))
                .createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());
    }

    // test case: Request the list of all attachments status when the facade returns an non-empty list. 
    // Check if the request response is compatible with the value produced by facade.
    @Test
    public void testGetAllAttachmentsStatus() throws Exception {
        InstanceStatus AttachmentStatus1 = new InstanceStatus("fake-Id-1", "fake-provider", InstanceState.IN_USE);
        InstanceStatus AttachmentStatus2 = new InstanceStatus("fake-Id-2", "fake-provider", InstanceState.IN_USE);
        InstanceStatus AttachmentStatus3 = new InstanceStatus("fake-Id-3", "fake-provider", InstanceState.IN_USE);

        List<InstanceStatus> AttachmentStatusList =
                Arrays.asList(AttachmentStatus1, AttachmentStatus2, AttachmentStatus3);
        Mockito.doReturn(AttachmentStatusList)
                .when(this.facade)
                .getAllInstancesStatus(Mockito.anyString(), Mockito.any(ResourceType.class));

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET,
                        ATTACHMENT_ENDPOINT.concat("/status"),
                        getHttpHeaders(), "");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<InstanceStatus>> token = new TypeToken<List<InstanceStatus>>() {
        };
        List<InstanceStatus> resultList =
                new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        Assert.assertEquals(3, resultList.size());

        Mockito.verify(this.facade, Mockito.times(1))
                .getAllInstancesStatus(Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Request an attachment by his id and test successfully return. 
    // Check if the request response is compatible with the value produced by facade.
    @Test
    public void testGetAttachmentById() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;

        AttachmentInstance attachmentInstance = new AttachmentInstance(fakeId);
        Mockito.doReturn(attachmentInstance).when(this.facade)
                .getAttachment(Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, attachmentIdEndpoint, getHttpHeaders(), "");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        AttachmentInstance resultAttachmentInstance =
                new Gson()
                        .fromJson(
                                result.getResponse().getContentAsString(),
                                AttachmentInstance.class);

        Assert.assertEquals(attachmentInstance, resultAttachmentInstance);

        Mockito.verify(this.facade, Mockito.times(1))
                .getAttachment(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request an attachment by his id when the instance is not found. 
    // Check if the request response is compatible with the value produced by facade.
    @Test
    public void testGetNotFoundAttachmentById() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;
        Mockito.doThrow(new InstanceNotFoundException())
                .when(this.facade)
                .getAttachment(Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, attachmentIdEndpoint, getHttpHeaders(), "");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1))
                .getAttachment(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Delete an attachment by his id and test successfully return. 
    // Check if the request response is compatible with the value produced by facade.
    @Test
    public void testDeleteExistingAttachment() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;
        Mockito.doNothing().when(this.facade)
                .deleteAttachment(Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.DELETE, attachmentIdEndpoint, getHttpHeaders(), "");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1))
                .deleteAttachment(Mockito.anyString(), Mockito.anyString());
    }


    // test case: Delete a not found attachment by his id and test fail return. 
    // Check the response of request and the call of facade for delete the attachment.
    @Test
    public void testDeleteNotFoundAttachment() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;
        Mockito.doThrow(new InstanceNotFoundException())
                .when(this.facade)
                .deleteAttachment(Mockito.anyString(), Mockito.anyString());
        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.DELETE, attachmentIdEndpoint, getHttpHeaders(), "");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(1))
                .deleteAttachment(Mockito.anyString(), Mockito.anyString());
    }

    private RequestBuilder createRequestBuilder(
            HttpMethod method, String urlTemplate, HttpHeaders headers, String body) {
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
                        .accept(MediaType.APPLICATION_JSON);
            case DELETE:
                return MockMvcRequestBuilders.delete(urlTemplate).headers(headers);
            default:
                return null;
        }
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(
                AttachmentOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY,
                fakeFederationTokenValue);
        return headers;
    }
}
