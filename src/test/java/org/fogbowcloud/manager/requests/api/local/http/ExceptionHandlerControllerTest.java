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
}
