package cloud.fogbow.ras.requests.api.local.http;

import cloud.fogbow.ras.api.http.Compute;
import cloud.fogbow.ras.api.http.HttpExceptionToErrorConditionTranslator;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ExceptionHandlerControllerTest {

    private final String COMPUTE_ENDPOINT = "/" + Compute.COMPUTE_ENDPOINT + "/";
    private final String URI_COMPUTE_ENDPOINT = "uri=/" + Compute.COMPUTE_ENDPOINT + "/";

    private MockMvc mockMvc;

    private Compute compute;

    @Before
    public void setup() {
        this.compute = Mockito.mock(Compute.class);
        this.mockMvc =
                MockMvcBuilders.standaloneSetup(compute)
                        .setControllerAdvice(new HttpExceptionToErrorConditionTranslator())
                        .build();
    }
}
