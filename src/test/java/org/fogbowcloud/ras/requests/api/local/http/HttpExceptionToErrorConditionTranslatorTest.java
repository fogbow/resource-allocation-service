package org.fogbowcloud.ras.requests.api.local.http;

import org.fogbowcloud.ras.api.http.ComputeOrdersController;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    //test case:
    @Test
    public void testHttpExceptionToErrorConditionTranslator() {
        // TODO
        //set up

        //exercise

        //verify
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