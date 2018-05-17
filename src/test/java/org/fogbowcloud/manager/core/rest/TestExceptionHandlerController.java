package org.fogbowcloud.manager.core.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.fogbowcloud.manager.core.plugins.identity.exceptions.InvalidCredentialsException;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.InvalidTokenException;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.rest.controllers.ComputeOrdersController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class TestExceptionHandlerController {
	
	private MockMvc mockMvc;
	
	private ComputeOrdersController computeOrdersController;
	
	@Before
	public void setup() {
		computeOrdersController = Mockito.mock(ComputeOrdersController.class);
		this.mockMvc = MockMvcBuilders.standaloneSetup(computeOrdersController)
				.setControllerAdvice(new ExceptionHandlerController())
				.build();
	}
	
	@Test
	public void testInvalidCredentialsException() throws Exception {
		String fakeAccessId = "fake-access-id";
		Mockito.when(computeOrdersController.getAllCompute(fakeAccessId)).thenThrow(new InvalidCredentialsException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getContentAsString());

		assertEquals(jsonObject.get("details"), "uri=/compute/");
		assertEquals(jsonObject.get("message"), "Invalid Credentials");
		assertEquals(jsonObject.get("statusCode"), HttpStatus.UNAUTHORIZED.name());
		assertEquals(Integer.toString(response.getStatus()), HttpStatus.UNAUTHORIZED.toString());
	}
	
	@Test
	public void testInvalidTokenException() throws Exception {
		String fakeAccessId = "fake-access-id";
		Mockito.when(computeOrdersController.getAllCompute(fakeAccessId)).thenThrow(new InvalidTokenException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getContentAsString());

		assertEquals(jsonObject.get("details"), "uri=/compute/");
		assertEquals(jsonObject.get("message"), "Invalid token");
		assertEquals(jsonObject.get("statusCode"), HttpStatus.UNAUTHORIZED.name());
		assertEquals(Integer.toString(response.getStatus()), HttpStatus.UNAUTHORIZED.toString());
	}
	
	@Test
	public void testTokenCreationException() throws Exception {
		String fakeAccessId = "fake-access-id";
		Mockito.when(computeOrdersController.getAllCompute(fakeAccessId)).thenThrow(new TokenCreationException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getContentAsString());

		assertEquals(jsonObject.get("details"), "uri=/compute/");
		assertEquals(jsonObject.get("message"), "Token Creation Exception");
		assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());
		assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
	}
	
	@Test
	public void testAnyException() throws Exception {
		String fakeAccessId = "fake-access-id";
		Mockito.when(computeOrdersController.getAllCompute(fakeAccessId)).thenThrow(new RuntimeException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getContentAsString());

		assertEquals(jsonObject.get("details"), "uri=/compute/");
		assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());
		
		assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
	}
}
