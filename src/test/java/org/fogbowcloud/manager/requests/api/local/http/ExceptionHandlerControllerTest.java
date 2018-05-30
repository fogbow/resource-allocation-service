package org.fogbowcloud.manager.requests.api.local.http;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.fogbowcloud.manager.api.local.http.ComputeOrdersController;
import org.fogbowcloud.manager.api.local.http.ExceptionTranslator;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.InvalidCredentialsException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.InvalidTokenException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ExceptionHandlerControllerTest {

	private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private MockMvc mockMvc;

    private ComputeOrdersController computeOrdersController;

    @Before
    public void setup() {
        this.computeOrdersController = Mockito.mock(ComputeOrdersController.class);
        this.mockMvc =
                MockMvcBuilders.standaloneSetup(computeOrdersController)
                        .setControllerAdvice(new ExceptionTranslator())
                        .build();
    }

    @Ignore
    @Test
    public void testInvalidCredentialsException() throws Exception {
        Mockito.when(this.computeOrdersController.getAllCompute(Mockito.anyString()))
                .thenThrow(new InvalidCredentialsException());

        MockHttpServletResponse response =
        		this.mockMvc.perform(
                                get("/compute/")
                                        .accept(MediaType.APPLICATION_JSON)
                                        .header(FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();

        JSONObject jsonObject = new JSONObject(response.getContentAsString());

        assertEquals(jsonObject.get("details"), "uri=/compute/");
        assertEquals(jsonObject.get("message"), "Invalid Credentials");
        assertEquals(jsonObject.get("statusCode"), HttpStatus.UNAUTHORIZED.name());
        assertEquals(Integer.toString(response.getStatus()), HttpStatus.UNAUTHORIZED.toString());
    }

    @Test
    public void testInvalidTokenException() throws Exception {
        Mockito.when(computeOrdersController.getAllCompute(Mockito.anyString()))
                .thenThrow(new InvalidTokenException());

        MockHttpServletResponse response =
                mockMvc.perform(
                                get("/compute/")
                                        .accept(MediaType.APPLICATION_JSON)
                                        .header(FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();

        JSONObject jsonObject = new JSONObject(response.getContentAsString());

        assertEquals(jsonObject.get("details"), "uri=/compute/");
        assertEquals(jsonObject.get("message"), "Invalid token");
        assertEquals(jsonObject.get("statusCode"), HttpStatus.UNAUTHORIZED.name());
        assertEquals(Integer.toString(response.getStatus()), HttpStatus.UNAUTHORIZED.toString());
    }

    @Test
    public void testTokenCreationException() throws Exception {
        Mockito.when(computeOrdersController.getAllCompute(Mockito.anyString()))
                .thenThrow(new TokenCreationException());

        MockHttpServletResponse response =
                mockMvc.perform(
                                get("/compute/")
                                        .accept(MediaType.APPLICATION_JSON)
                                        .header(FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();

        JSONObject jsonObject = new JSONObject(response.getContentAsString());

        assertEquals(jsonObject.get("details"), "uri=/compute/");
        assertEquals(jsonObject.get("message"), "Token Creation Exception");
        assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());
        assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
    }

    @Test
    public void testAnyException() throws Exception {
        Mockito.when(computeOrdersController.getAllCompute(Mockito.anyString()))
                .thenThrow(new RuntimeException());

        MockHttpServletResponse response =
                mockMvc.perform(
                                get("/compute/")
                                        .accept(MediaType.APPLICATION_JSON)
                                        .header(FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();

        JSONObject jsonObject = new JSONObject(response.getContentAsString());
        assertEquals(jsonObject.get("details"), "uri=/compute/");
        assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());

        assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
    }
}
