package org.fogbowcloud.manager.requests.api.local.http;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.fogbowcloud.manager.api.http.ComputeOrdersController;
import org.fogbowcloud.manager.api.http.HttpExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ExceptionHandlerControllerTest {

    private final String COMPUTE_ENDPOINT = "/" + ComputeOrdersController.COMPUTE_ENDPOINT + "/";
    private final String URI_COMPUTE_ENDPOINT = "uri=/" + ComputeOrdersController.COMPUTE_ENDPOINT + "/";

    private MockMvc mockMvc;

    private ComputeOrdersController computeOrdersController;

    @Before
    public void setup() {
        this.computeOrdersController = Mockito.mock(ComputeOrdersController.class);
        this.mockMvc =
                MockMvcBuilders.standaloneSetup(computeOrdersController)
                        .setControllerAdvice(new HttpExceptionToErrorConditionTranslator())
                        .build();
    }

    @Test
    public void testUnauthorizedException() throws Exception {
        Mockito.when(computeOrdersController.getAllComputes(Mockito.anyString()))
                .thenThrow(new UnauthorizedRequestException());

        MockHttpServletResponse response =
                mockMvc.perform(
                                get(COMPUTE_ENDPOINT)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .header(ComputeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();
        JSONObject jsonObject = new JSONObject(response.getContentAsString());

        assertEquals(jsonObject.get("details"), URI_COMPUTE_ENDPOINT);
        assertEquals(jsonObject.get("message"), "Unauthorized Error");
        assertEquals(response.getStatus(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void testUnauthenticatedException() throws Exception {
        Mockito.when(this.computeOrdersController.getAllComputes(Mockito.anyString()))
                .thenThrow(new UnauthenticatedUserException());

        MockHttpServletResponse response =
                this.mockMvc.perform(
                        get(COMPUTE_ENDPOINT)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(ComputeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();

        JSONObject jsonObject = new JSONObject(response.getContentAsString());

        assertEquals(jsonObject.get("details"), URI_COMPUTE_ENDPOINT);
        assertEquals(jsonObject.get("message"), "Unauthenticated error");
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED.value());
        assertEquals(Integer.toString(response.getStatus()), HttpStatus.UNAUTHORIZED.toString());
    }

//    @Test
//    public void testTokenCreationException() throws Exception {
//        Mockito.when(computeOrdersController.getAllCompute(Mockito.anyString()))
//                .thenThrow(new TokenValueCreationException());
//
//        MockHttpServletResponse response =
//                mockMvc.perform(
//                                get(COMPUTE_ENDPOINT)
//                                        .accept(MediaType.APPLICATION_JSON)
//                                        .header(ComputeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
//                        .andReturn()
//                        .getResponse();
//
//        JSONObject jsonObject = new JSONObject(response.getContentAsString());
//
//        assertEquals(jsonObject.get("details"), URI_COMPUTE_ENDPOINT);
//        assertEquals(jsonObject.get("message"), "Token Creation Exception");
//        assertEquals(jsonObject.get("statusCode"), HttpStatus.BAD_REQUEST.name());
//        assertEquals(Integer.toString(response.getStatus()), HttpStatus.BAD_REQUEST.toString());
//    }

    @Test
    public void testAnyException() throws Exception {
        Mockito.when(computeOrdersController.getAllComputes(Mockito.anyString()))
                .thenThrow(new RuntimeException());

        MockHttpServletResponse response =
                mockMvc.perform(
                                get(COMPUTE_ENDPOINT)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .header(ComputeOrdersController.FEDERATION_TOKEN_VALUE_HEADER_KEY, Mockito.anyString()))
                        .andReturn()
                        .getResponse();

        JSONObject jsonObject = new JSONObject(response.getContentAsString());
        assertEquals(jsonObject.get("details"), URI_COMPUTE_ENDPOINT);
        assertEquals(response.getStatus(), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        assertEquals(Integer.toString(response.getStatus()), HttpStatus.UNSUPPORTED_MEDIA_TYPE.toString());
    }
}
