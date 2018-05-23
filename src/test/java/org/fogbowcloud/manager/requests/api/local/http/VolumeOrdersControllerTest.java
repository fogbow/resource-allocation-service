package org.fogbowcloud.manager.requests.api.local.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import org.fogbowcloud.manager.api.local.http.VolumeOrdersController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = VolumeOrdersController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class VolumeOrdersControllerTest {

    public static final String CORRECT_BODY =
            "{\"federationToken\": null, \"requestingMember\":\"req-member\", \"providingMember\":\"prov-member\", \"volumeSize\": 1, \"type\":\"volume\"}";

    public static final String VOLUME_END_POINT = "/volume";

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() throws UnauthorizedException, OrderManagementException {
        this.facade = spy(ApplicationFacade.class);
    }

    @Test
    public void createdVolumeTest() throws Exception {
        mockApplicationContrllerToCreateVolume();

        HttpHeaders headers = getHttpHeaders();

        RequestBuilder requestBuilder = createRequestBuilder(headers, CORRECT_BODY);

        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();

        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    private RequestBuilder createRequestBuilder(HttpHeaders headers, String body) {
        return MockMvcRequestBuilders.post(VOLUME_END_POINT).headers(headers)
                .accept(MediaType.APPLICATION_JSON).content(body)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private void mockApplicationContrllerToCreateVolume()
            throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
        doNothing().when(this.facade).createVolume(any(VolumeOrder.class), anyString());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }

}
