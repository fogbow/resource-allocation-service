package org.fogbowcloud.manager.requests.api.local.http;

import static org.mockito.BDDMockito.given;

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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

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

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowManagerException {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    // test case: Request a attachment creation and test successfully return. 
    // Check the response of request and the call of facade for create the attachment.
    @Test
    public void CreateAttachmenTest() throws Exception {
    	
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

    
    // test case: Request a attachment creation with 3 types of wrong body and test fail return
    // Check the response of request and the call of facade for create the attachment.
    @Test
    public void wrongBodyToPostAttachmentTest() throws Exception {
        // Case 1: Empty json
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

        // Case 2: Invalid json
        // set up
        requestBuilder =
                createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), "{}");

        // exercise: Make the request
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1))
    		.createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());

        // Case 3: Json with empty properties.
        // set up
        requestBuilder =
                createRequestBuilder(
                        HttpMethod.POST,
                        ATTACHMENT_ENDPOINT,
                        getHttpHeaders(),
                        BODY_WITH_EMPTY_PROPERTIES);

        // exercise: Make the request
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.atLeast(2))  // for the two tests where a non-empty body was provided
    		.createAttachment(Mockito.any(AttachmentOrder.class), Mockito.anyString());
    }

    // test case: Request the list of all attachments when the facade returns an empty list. 
    // Check the response of request and the call of facade for get the attachments. 
    @Test
    public void getAllAttachmentsWhenHasNoData() throws Exception {
    	//set up
        List<AttachmentInstance> attachementInstanceList = new ArrayList<>();
        Mockito.doReturn(attachementInstanceList).when(this.facade).getAllAttachments(Mockito.anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET, ATTACHMENT_ENDPOINT, getHttpHeaders(), "");

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        
        String expectedResult = "[]";

        Assert.assertEquals(expectedResult, result.getResponse().getContentAsString());
        
        Mockito.verify(this.facade, Mockito.times(1))
			.getAllAttachments(Mockito.anyString());
    }

    // test case: Request the list of all attachments when the facade returns an non-empty list. 
    // Check the response of request and the call of facade for get the attachments. 
    @Test
    public void getAllAttachmentsWhenHasData() throws Exception {
    	// set up
        AttachmentInstance AttachmentInstance1 = new AttachmentInstance("fake-Id-1");
        AttachmentInstance AttachmentInstance2 = new AttachmentInstance("fake-Id-2");
        AttachmentInstance AttachmentInstance3 = new AttachmentInstance("fake-Id-3");

        List<AttachmentInstance> AttachmentInstanceList =
                Arrays.asList(
                        new AttachmentInstance[] {
                            AttachmentInstance1, AttachmentInstance2, AttachmentInstance3
                        });
        Mockito.doReturn(AttachmentInstanceList).when(this.facade)
        	.getAllAttachments(Mockito.anyString());

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET,
                		ATTACHMENT_ENDPOINT,
                		getHttpHeaders(), "");
        
        // exercise: Make the request.
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<AttachmentInstance>> token = new TypeToken<List<AttachmentInstance>>() {};
        List<AttachmentInstance> resultList =
                new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        Assert.assertTrue(resultList.size() == 3);
        
        Mockito.verify(this.facade, Mockito.times(1))
			.getAllAttachments(Mockito.anyString());
    }
    
    // test case: Request the list of all attachments status when the facade returns an non-empty list. 
    // Check the response of request and the call of facade for get the attachments status.
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
        Mockito.doReturn(AttachmentStatusList)
        	.when(this.facade)
        	.getAllInstancesStatus(Mockito.anyString(), Mockito.any(InstanceType.class));

        RequestBuilder requestBuilder =
                createRequestBuilder(HttpMethod.GET,
                		ATTACHMENT_ENDPOINT.concat("/status"),
                		getHttpHeaders(), "");
        
        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<InstanceStatus>> token = new TypeToken<List<InstanceStatus>>() {};
        List<InstanceStatus> resultList =
                new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        Assert.assertTrue(resultList.size() == 3);
        
        Mockito.verify(this.facade, Mockito.times(1))
			.getAllInstancesStatus(Mockito.anyString(), Mockito.any(InstanceType.class));
    }

    // test case: Request a attachment by his id and test successfully return. 
    // Check the response of request and the call of facade for get the compute.
    @Test
    public void getAttachmentById() throws Exception {
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
        
        Assert.assertTrue(resultAttachmentInstance != null);
        
        Mockito.verify(this.facade, Mockito.times(1))
			.getAttachment(Mockito.anyString() ,Mockito.anyString());
    }

    // test case: Request a attachment by his id when the instance is not found. 
    // Check the response of request and the call of facade for get the attachment.
    @Test
    public void getNotFoundAttachmentById() throws Exception {
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
			.getAttachment(Mockito.anyString() ,Mockito.anyString());
    }

    // test case: Delete a attachment by his id and test successfully return. 
    // Check the response of request and the call of facade for delete the attachment.
    @Test
    public void deleteExistingAttachement() throws Exception {
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
			.deleteAttachment(Mockito.anyString() ,Mockito.anyString());
    }

    
    // test case: Delete a not found attachment by his id and test fail return. 
    // Check the response of request and the call of facade for delete the attachment.
    @Test
    public void deleteNotFoundAttachmentById() throws Exception {
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
			.deleteAttachment(Mockito.anyString() ,Mockito.anyString());
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
