package org.fogbowcloud.ras.requests.api.local.http;

import org.fogbowcloud.ras.api.http.TokenRequestController;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.junit.Assert;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = TokenRequestController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class TokenRequestControllerTest {
    private static final String CORRECT_BODY =
            "{\"username\" : \"user-name-value\",\"password\" : \"password-value\"}";
    private static final String TOKEN_END_POINT = "/" + TokenRequestController.TOKEN_ENDPOINT;

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
    }

    // test case: When calling the createTokenValue() method with the correct credentials, it must return a token value
    // string and an HttpStatus equal to Created.
    @Test
    public void createdTokenValueTest() throws Exception {
        // set up
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);

        // exercise
        Mockito.doReturn("tokenValue").when(this.facade)
                .createTokenValue(Mockito.anyMap());

        HttpHeaders headers = new HttpHeaders();

        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_END_POINT)
                .headers(headers).accept(MediaType.APPLICATION_JSON).content(CORRECT_BODY)
                .contentType(MediaType.APPLICATION_JSON)).andReturn();

        int expectedStatus = HttpStatus.CREATED.value();

        String resultTokenValue = result.getResponse().getContentAsString();

        // verify
        Mockito.verify(this.facade, times(1)).createTokenValue(Mockito.anyMap());

        Assert.assertEquals("tokenValue", resultTokenValue);
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
    }
}
