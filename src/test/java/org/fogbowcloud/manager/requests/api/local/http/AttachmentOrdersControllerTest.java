package org.fogbowcloud.manager.requests.api.local.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.fogbowcloud.manager.api.http.AttachmentOrdersController;
import org.fogbowcloud.manager.api.http.ComputeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
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

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = AttachmentOrdersControllerTest.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class AttachmentOrdersControllerTest {
	   private final String CORRECT_BODY = "{"
	   		+ "\"source\": \"b8852ff6-ce00-45aa-898d-ddaffb5c6173\","
	   		+ "\"target\": \"596f93c7-06a1-4621-8c9d-5330a089eafe\","
	   		+ "\"device\": \"/dev/sdd\""
	   		+ "}";

	    private final String BODY_WITH_EMPY_FIELDS = "{"
		   		+ "\"source\": \"\","
		   		+ "\"target\": \"\","
		   		+ "\"device\": \"\"}";

	    private final String ATTACHMENT_ENDPOINT = "/".concat(AttachmentOrdersController.ATTACHMENT_ENDPOINT);

	    @Autowired
	    private MockMvc mockMvc;

	    private ApplicationFacade facade;


	    @Before
	    public void setUp() throws FogbowManagerException {
	        this.facade = spy(ApplicationFacade.class);
	        PowerMockito.mockStatic(ApplicationFacade.class);
	        given(ApplicationFacade.getInstance()).willReturn(this.facade);
	    }

	    @Test
	    public void createAttachementTest() throws Exception {
	        String orderId = "fake-id";

	        doReturn(orderId).when(this.facade).createAttachment(any(AttachmentOrder.class), anyString());

	        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), CORRECT_BODY);

	        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

	        int expectedStatus = HttpStatus.CREATED.value();
	        assertEquals(expectedStatus, result.getResponse().getStatus());
	    }

	    @Test
	    public void wrongBodyToPostComputeTest() throws Exception {
	        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), "");

	        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

	        int expectedStatus = HttpStatus.BAD_REQUEST.value();
	        assertEquals(expectedStatus, result.getResponse().getStatus());
	        
	        requestBuilder = createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), "{}");

	        result = this.mockMvc.perform(requestBuilder).andReturn();

	        expectedStatus = HttpStatus.BAD_REQUEST.value();
	        assertEquals(expectedStatus, result.getResponse().getStatus());
	        
	        requestBuilder = createRequestBuilder(HttpMethod.POST, ATTACHMENT_ENDPOINT, getHttpHeaders(), BODY_WITH_EMPY_FIELDS);

	        result = this.mockMvc.perform(requestBuilder).andReturn();

	        expectedStatus = HttpStatus.BAD_REQUEST.value();
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
