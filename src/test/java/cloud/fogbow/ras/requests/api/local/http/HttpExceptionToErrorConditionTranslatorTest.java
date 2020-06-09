package cloud.fogbow.ras.requests.api.local.http;

import cloud.fogbow.common.exceptions.DependencyDetectedException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.OnGoingOperationException;
import cloud.fogbow.ras.requests.api.local.http.util.PojoController;
import cloud.fogbow.ras.requests.api.local.http.util.PojoService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(SpringRunner.class)
@WebMvcTest(PojoController.class)
// TODO (chico) - Finish implementation
public class HttpExceptionToErrorConditionTranslatorTest {

    private final String POJO_CONTROLER_REQUEST_SUFIX = "/";
    private final String EXCEPTION_MESSAGE_DEFAULT = "EXCEPTION_MESSAGE_DEFAULT";
    private final int IT_SHOULD_NEVER_HAPPEN = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PojoService service;

    private RequestBuilder requestBuilder;

    @Before
    public void setUp() {
        this.requestBuilder = createRequestBuilder();
    }

    // test case: When any request is performed and throws a FogbowException,
    // it must verify if it return a UNSUPPORTED_MEDIA_TYPE status code.
    @Test
    public void testTranslationWhenIsFogbowException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        FogbowException exceptionThrown = new FogbowException(exceptionMessage);
        int statusCodeExpected = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a InstanceNotFoundException,
    // it must verify if it return a NOT_FOUND status code.
    @Test
    public void testTranslationWhenIsInstanceNotFoundException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        InstanceNotFoundException exceptionThrown = new InstanceNotFoundException(exceptionMessage);
        int statusCodeExpected = HttpStatus.NOT_FOUND.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a DependencyDetectedException,
    // it must verify if it return a UNSUPPORTED_MEDIA_TYPE status code.
    @Test
    public void testTranslationWhenIsDependencyDetectedException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        DependencyDetectedException exceptionThrown = new DependencyDetectedException(exceptionMessage);
        int statusCodeExpected = IT_SHOULD_NEVER_HAPPEN;

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a OnGoingOperationException,
    // it must verify if it return a UNSUPPORTED_MEDIA_TYPE status code.
    @Test
    public void testTranslationWhenIsOnGoingOperationException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        OnGoingOperationException exceptionThrown = new OnGoingOperationException(exceptionMessage);
        int statusCodeExpected = IT_SHOULD_NEVER_HAPPEN;

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    private <T extends Exception> void checkExceptionToCodeResponse(T exception, int statusCodeExpected)
            throws Exception {

        //set up
        Mockito.doThrow(exception).when(this.service).throwException();

        //exercise
        MvcResult result = this.mockMvc.perform(this.requestBuilder).andReturn();

        //verify
        MockHttpServletResponse response = result.getResponse();
        Assert.assertEquals(statusCodeExpected, response.getStatus());
        Assert.assertTrue(response.getContentAsString().contains(EXCEPTION_MESSAGE_DEFAULT));
    }

    private RequestBuilder createRequestBuilder() {
        return MockMvcRequestBuilders.get(POJO_CONTROLER_REQUEST_SUFIX);
    }

}
