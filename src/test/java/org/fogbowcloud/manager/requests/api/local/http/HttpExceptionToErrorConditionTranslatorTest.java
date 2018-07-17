package org.fogbowcloud.manager.requests.api.local.http;

import org.fogbowcloud.manager.api.http.ComputeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

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
	public void setUp() throws FogbowManagerException {
		this.facade = spy(ApplicationFacade.class);
		PowerMockito.mockStatic(ApplicationFacade.class);
		given(ApplicationFacade.getInstance()).willReturn(this.facade);
	}

	@Test
	public void testUnauthorizedRequestExceptionMapping() throws Exception {
		doThrow(new UnauthorizedRequestException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.FORBIDDEN.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testUnauthenticatedUserExceptionMapping() throws Exception {
		doThrow(new UnauthenticatedUserException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.UNAUTHORIZED.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testInvalidParameterExceptionMapping() throws Exception {
		doThrow(new InvalidParameterException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.BAD_REQUEST.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testInstanceNotFoundExceptionMapping() throws Exception {
		doThrow(new InstanceNotFoundException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.NOT_FOUND.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testQuotaExceededExceptionMapping() throws Exception {
		doThrow(new QuotaExceededException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.CONFLICT.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testNoAvailableResourcesExceptionMapping() throws Exception {
		doThrow(new NoAvailableResourcesException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.NOT_ACCEPTABLE.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testUnavailableProviderExceptionMapping() throws Exception {
		doThrow(new UnavailableProviderException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.GATEWAY_TIMEOUT.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testUnexpectedExceptionMapping() throws Exception {
		doThrow(new UnexpectedException()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
	}

	@Test
	public void testExceptionMapping() throws Exception {
		doThrow(new Exception()).when(this.facade).getAllComputes(anyString());

		RequestBuilder requestBuilder = createRequestBuilder(computeEndpoint, getHttpHeaders());
		MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

		int expectedStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
		assertEquals(expectedStatus, result.getResponse().getStatus());
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