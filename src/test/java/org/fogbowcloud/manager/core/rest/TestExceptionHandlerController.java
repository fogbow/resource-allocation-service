package org.fogbowcloud.manager.core.rest;

import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsException;
import org.fogbowcloud.manager.core.exceptions.InvalidTokenException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
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

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

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
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new InvalidCredentialsException());
		
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
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new InvalidTokenException());
		
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
	public void testUnexpectedException() throws Exception {
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new UnexpectedException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getContentAsString());

		assertEquals(jsonObject.get("details"), "uri=/compute/");
		assertEquals(jsonObject.get("message"), "Unexpected Exception");
		assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());
		assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
	}
	
	@Test
	public void testAnyException() throws Exception {
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new RuntimeException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getContentAsString());

		assertEquals(jsonObject.get("details"), "uri=/compute/");
		assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());
		
		assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
	}
}
