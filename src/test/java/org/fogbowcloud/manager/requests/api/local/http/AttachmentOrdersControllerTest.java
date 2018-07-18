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

import org.fogbowcloud.manager.api.http.AttachmentOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
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
@WebMvcTest(value = AttachmentOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class AttachmentOrdersControllerTest {
    private final String ATTACHMENT_ENDPOINT =
            "/".concat(AttachmentOrdersController.ATTACHMENT_ENDPOINT);

    private final String CORRECT_BODY =
            "{"
                    + "\"source\": \"b8852ff6-ce00-45aa-898d-ddaffb5c6173\","
                    + "\"target\": \"596f93c7-06a1-4621-8c9d-5330a089eafe\","
                    + "\"device\": \"/dev/sdd\""
                    + "}";

    private final String BODY_WITH_EMPTY_PROPERTIES =
            "{"
                    + "\"source\": \"b8852ff6-ce00-45aa-898d-ddaffb5c6173\","
                    + "\"target\": \"596f93c7-06a1-4621-8c9d-5330a089eafe\","
                    + "\"device\": \"/dev/sdd\""
                    + "}";

    @Autowired private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowManagerException {
        this.facade = spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    @Test
    public void CreateAttachmenTest() throws Exception {
        String orderId = "fake-id";

        doReturn(orderId)
                .when(this.facade)
                .createAttachment(any(AttachmentOrder.class), anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(
                        HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void wrongBodyToPostAttachmentTest() throws Exception {
        // Empty json
        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), "{}");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        // Invalid json

        requestBuilder =
                createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), "{}");

        result = this.mockMvc.perform(requestBuilder).andReturn();

        expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        // Json with empty properties.

        requestBuilder =
                createRequestBuilder(
                        HttpMethod.POST,
                        ATTACHMENT_ENDPOINT,
                        getHttpHeaders(),
                        BODY_WITH_EMPTY_PROPERTIES);

        result = this.mockMvc.perform(requestBuilder).andReturn();

        expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getAllAttachmentsWhenHasNoData() throws Exception {
        List<AttachmentInstance> attachementInstanceList = new ArrayList<>();
        doReturn(attachementInstanceList).when(this.facade).getAllAttachments(anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, ATTACHMENT_ENDPOINT, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void getAllAttachmentsWhenHasData() throws Exception {
        AttachmentInstance AttachmentInstance1 = new AttachmentInstance("fake-Id-1");
        AttachmentInstance AttachmentInstance2 = new AttachmentInstance("fake-Id-2");
        AttachmentInstance AttachmentInstance3 = new AttachmentInstance("fake-Id-3");

        List<AttachmentInstance> AttachmentInstanceList =
                Arrays.asList(
                        new AttachmentInstance[] {
                            AttachmentInstance1, AttachmentInstance2, AttachmentInstance3
                        });
        doReturn(AttachmentInstanceList).when(this.facade).getAllAttachments(anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, ATTACHMENT_ENDPOINT, getHttpHeaders(), "");
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<AttachmentInstance>> token = new TypeToken<List<AttachmentInstance>>() {};
        List<AttachmentInstance> resultList =
                new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        assertTrue(resultList.size() == 3);
    }
    
    @Test
    public void getAllAttachmentsStatus() throws Exception {
    	InstanceStatus AttachmentStatus1 = new InstanceStatus("fake-Id-1", "fake-provider", InstanceState.IN_USE);
    	InstanceStatus AttachmentStatus2 = new InstanceStatus("fake-Id-2", "fake-provider", InstanceState.IN_USE);
    	InstanceStatus AttachmentStatus3 = new InstanceStatus("fake-Id-3", "fake-provider", InstanceState.IN_USE);
    	
        List<InstanceStatus> AttachmentStatusList =
                Arrays.asList(
                        new InstanceStatus[] {
                        		AttachmentStatus1, AttachmentStatus2, AttachmentStatus3
                        });
        doReturn(AttachmentStatusList).when(this.facade).getAllInstancesStatus(anyString(), any(InstanceType.class));

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, ATTACHMENT_ENDPOINT.concat("/status"), getHttpHeaders(), "");
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<InstanceStatus>> token = new TypeToken<List<InstanceStatus>>() {};
        List<InstanceStatus> resultList =
                new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        assertTrue(resultList.size() == 3);
    }

    @Test
    public void getAttachmentById() throws Exception {
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;

        AttachmentInstance attachmentInstance = new AttachmentInstance(fakeId);
        doReturn(attachmentInstance).when(this.facade).getAttachment(anyString(), anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, attachmentIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        AttachmentInstance resultAttachmentInstance =
                new Gson()
                        .fromJson(
                                result.getResponse().getContentAsString(),
                                AttachmentInstance.class);
        assertTrue(resultAttachmentInstance != null);
    }

    @Test
    public void getNotFoundAttachmentById() throws Exception {
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;
        doThrow(new InstanceNotFoundException())
                .when(this.facade)
                .getAttachment(anyString(), anyString());
        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, attachmentIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.NOT_FOUND.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void deleteExistingAttachement() throws Exception {
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;
        doNothing().when(this.facade).deleteAttachment(anyString(), anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.DELETE, attachmentIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Test
    public void deleteNotFoundAttachmentById() throws Exception {
        String fakeId = "fake-Id-1";
        String attachmentIdEndpoint = ATTACHMENT_ENDPOINT + "/" + fakeId;
        doThrow(new InstanceNotFoundException())
                .when(this.facade)
                .deleteAttachment(anyString(), anyString());
        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.DELETE, attachmentIdEndpoint, getHttpHeaders(), "");

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.NOT_FOUND.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
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
