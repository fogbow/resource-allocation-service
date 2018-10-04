package org.fogbowcloud.ras.requests.api.local.http;

import org.fogbowcloud.ras.api.http.ComputeOrdersController;
import org.fogbowcloud.ras.api.http.HttpExceptionToErrorConditionTranslator;
import org.junit.Before;
import org.mockito.Mockito;
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
