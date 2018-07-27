package org.fogbowcloud.manager.requests.api.local.http;

import org.fogbowcloud.manager.api.http.ComputeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.*;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = ComputeOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class HttpExceptionToErrorConditionTranslatorTest {

	@Autowired
	private MockMvc mockMvc;
	private ApplicationFacade facade;

	private final String computeEndpoint = "/" + ComputeOrdersController.COMPUTE_ENDPOINT;

	@Before
	public void setUp() {
		this.facade = Mockito.spy(ApplicationFacade.class);
		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
	}

	//test case: Tests if an UnauthorizedRequestException will be mapped to a FORBIDDEN http status
	@Test
	public void testUnauthorizedRequestExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.FORBIDDEN.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if an UnauthenticatedUserException will be mapped to an UNAUTHORIZED http status
	@Test
	public void testUnauthenticatedUserExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.UNAUTHORIZED.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if an InvalidParameterException will be mapped to a BAD_REQUEST http status
	@Test
	public void testInvalidParameterExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new InvalidParameterException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.BAD_REQUEST.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if an InstanceNotFoundException will be mapped to a NOT_FOUND http status
	@Test
	public void testInstanceNotFoundExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new InstanceNotFoundException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.NOT_FOUND.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if a QuotaExceededException will be mapped to a CONFLICT http status
	@Test
	public void testQuotaExceededExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new QuotaExceededException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.CONFLICT.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if a NoAvailableResourcesException will be mapped to a NOT_ACCEPTABLE http status
	@Test
	public void testNoAvailableResourcesExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new NoAvailableResourcesException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.NOT_ACCEPTABLE.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if an UnavailableProviderException will be mapped to a GATEWAY_TIMEOUT http status
	@Test
	public void testUnavailableProviderExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new UnavailableProviderException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.GATEWAY_TIMEOUT.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if an UnexpectedException will be mapped to an INTERNAL_SERVER_ERROR http status
	@Test
	public void testUnexpectedExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new UnexpectedException()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	//test case: Tests if an Exception will be mapped to an UNSUPPORTED_MEDIA_TYPE http status
	@Test
	public void testExceptionMapping() throws Exception {
		//set up
		Mockito.doThrow(new Exception()).when(this.facade).getAllComputes(Mockito.anyString());
		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		//exercise
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
		//verify
		int expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	private RequestBuilder createRequestBuilder(String urlTemplate, HttpHeaders headers) {
		return MockMvcRequestBuilders.get(urlTemplate)
				.headers(headers)
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON);
	}

	private HttpHeaders getHttpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		String fakeFederationTokenValue = "fake-access-id";
		headers.set(ComputeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
		return headers;
	}
}