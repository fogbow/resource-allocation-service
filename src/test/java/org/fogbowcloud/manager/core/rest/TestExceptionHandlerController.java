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
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class TestExceptionHandlerController {
	
	private MockMvc mockMvc;
	
	private ComputeOrdersController computeOrdersController;
	
	private ObjectMapper mapper;
		
	@Before
	public void setup() {
		computeOrdersController = Mockito.mock(ComputeOrdersController.class);
		this.mockMvc = MockMvcBuilders.standaloneSetup(computeOrdersController)
				.setControllerAdvice(new ExceptionHandlerController())
				.build();
		this.mapper = new ObjectMapper();
	}
	
	@Test
	public void testInvalidCredentialsException() throws Exception {
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new InvalidCredentialsException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		ExceptionResponse r = mapper.readValue(response.getContentAsString(), ExceptionResponse.class);
		
		assertEquals(r.getDetails(), "uri=/compute/");
		assertEquals(r.getMessage(), "Invalid Credentials");
		assertEquals(r.getStatusCode(), HttpStatus.UNAUTHORIZED);
		assertEquals(new Integer(response.getStatus()).toString(), HttpStatus.UNAUTHORIZED.toString());
	}
	
	@Test
	public void testInvalidTokenException() throws Exception {
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new InvalidTokenException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		ExceptionResponse r = mapper.readValue(response.getContentAsString(), ExceptionResponse.class);
		
		assertEquals(r.getDetails(), "uri=/compute/");
		assertEquals(r.getMessage(), "Invalid token");
		assertEquals(r.getStatusCode(), HttpStatus.UNAUTHORIZED);
		assertEquals(new Integer(response.getStatus()).toString(), HttpStatus.UNAUTHORIZED.toString());
	}
	
	@Test
	public void testUnexpectedException() throws Exception {
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new UnexpectedException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		ExceptionResponse r = mapper.readValue(response.getContentAsString(), ExceptionResponse.class);
		
		assertEquals(r.getDetails(), "uri=/compute/");
		assertEquals(r.getMessage(), "Unexpected Exception");
		assertEquals(r.getStatusCode(), HttpStatus.BAD_REQUEST);
		assertEquals(new Integer(response.getStatus()).toString(), HttpStatus.BAD_REQUEST.toString());
	}
	
	@Test
	public void testAnyException() throws Exception {
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new RuntimeException());
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
        		.andReturn().getResponse();
		
		ExceptionResponse r = mapper.readValue(response.getContentAsString(), ExceptionResponse.class);
		
		assertEquals(r.getDetails(), "uri=/compute/");
		assertEquals(r.getStatusCode(), HttpStatus.BAD_REQUEST);
		assertEquals(new Integer(response.getStatus()).toString(), HttpStatus.BAD_REQUEST.toString());
	}
}
