package org.fogbowcloud.manager.core.rest;

import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class ComputeOrdersControllerTest {
	
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
	public void testInvalidCredentialsException () throws Exception {
		
		Mockito.when(computeOrdersController.getAllCompute()).thenThrow(new InvalidCredentialsException());
		
		Mockito.doThrow(new InvalidCredentialsException()).when(computeOrdersController).getAllCompute();
		
		MockHttpServletResponse response = mockMvc.perform(get("/compute/")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.firstName", is("teste")))
        		.andReturn().getResponse();
				
		System.out.println(response.getStatus());
		System.out.println(response.getContentType());
		//System.out.println(response.getContent.);
		Assert.assertEquals(expected, actual);
	}
	
}
