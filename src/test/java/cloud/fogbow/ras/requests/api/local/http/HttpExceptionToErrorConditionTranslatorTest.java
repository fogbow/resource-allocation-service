package cloud.fogbow.ras.requests.api.local.http;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.requests.api.local.http.util.TestController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({TestController.Mock.class})
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(TestController.class)
public class HttpExceptionToErrorConditionTranslatorTest {

    private final String POJO_CONTROLLER_REQUEST_SUFIX = "/";
    private final String EXCEPTION_MESSAGE_DEFAULT = "EXCEPTION_MESSAGE_DEFAULT";
    private final int IT_SHOULD_NEVER_HAPPEN = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();

    @Autowired
    private MockMvc mockMvc;

    private RequestBuilder requestBuilder;

    @Before
    public void setUp() {
        this.requestBuilder = createRequestBuilder();
    }

    // test case: When any request is performed and throws a InvalidParameterException,
    // it must verify if it return a BAD_REQUEST status code.
    @Test
    public void testTranslationWhenIsInvalidParameterException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        InvalidParameterException exceptionThrown = new InvalidParameterException(exceptionMessage);
        int statusCodeExpected = HttpStatus.BAD_REQUEST.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a UnauthenticatedUserException,
    // it must verify if it return a UNAUTHORIZED status code.
    @Test
    public void testTranslationWhenIsUnauthenticatedUserException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        UnauthenticatedUserException exceptionThrown = new UnauthenticatedUserException(exceptionMessage);
        int statusCodeExpected = HttpStatus.UNAUTHORIZED.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a UnavailableProviderException,
    // it must verify if it return a SERVICE_UNAVAILABLE status code.
    @Test
    public void testTranslationWhenIsUnavailableProviderException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        UnavailableProviderException exceptionThrown = new UnavailableProviderException(exceptionMessage);
        int statusCodeExpected = HttpStatus.SERVICE_UNAVAILABLE.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a UnauthorizedRequestException,
    // it must verify if it return a FORBIDDEN status code.
    @Test
    public void testTranslationWhenIsUnauthorizedRequestException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        UnauthorizedRequestException exceptionThrown = new UnauthorizedRequestException(exceptionMessage);
        int statusCodeExpected = HttpStatus.FORBIDDEN.value();

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

    // test case: When any request is performed and throws a ConfigurationErrorException,
    // it must verify if it return a CONFLICT status code.
    @Test
    public void testTranslationWhenIsConfigurationErrorException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        ConfigurationErrorException exceptionThrown = new ConfigurationErrorException(exceptionMessage);
        int statusCodeExpected = HttpStatus.CONFLICT.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a UnacceptableOperationException,
    // it must verify if it return a NOT_ACCEPTABLE status code.
    @Test
    public void testTranslationWhenIsUnacceptableOperationException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        UnacceptableOperationException exceptionThrown = new UnacceptableOperationException(exceptionMessage);
        int statusCodeExpected = HttpStatus.NOT_ACCEPTABLE.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws a InternalServerErrorException,
    // it must verify if it return a INTERNAL_SERVER_ERROR status code.
    @Test
    public void testTranslationWhenIsUnexpectedException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        InternalServerErrorException exceptionThrown = new InternalServerErrorException(exceptionMessage);
        int statusCodeExpected = HttpStatus.INTERNAL_SERVER_ERROR.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    // test case: When any request is performed and throws any Exception,
    // it must verify if it return a UNSUPPORTED_MEDIA_TYPE status code.
    @Test
    public void testTranslationWhenIsFogbowException() throws Exception {
        // set up
        String exceptionMessage = EXCEPTION_MESSAGE_DEFAULT;
        Exception exceptionThrown = new Exception(exceptionMessage);
        int statusCodeExpected = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();

        // exercise and verify
        checkExceptionToCodeResponse(exceptionThrown, statusCodeExpected);
    }

    private <T extends Exception> void checkExceptionToCodeResponse(T exception, int statusCodeExpected)
            throws Exception {

        //set up
        PowerMockito.mockStatic(TestController.Mock.class);
        PowerMockito.when(TestController.Mock.class, "throwException").thenThrow(exception);

        //exercise
        MvcResult result = this.mockMvc.perform(this.requestBuilder).andReturn();

        //verify
        MockHttpServletResponse response = result.getResponse();
        Assert.assertEquals(statusCodeExpected, response.getStatus());
        Assert.assertTrue(response.getContentAsString().contains(EXCEPTION_MESSAGE_DEFAULT));
    }

    private RequestBuilder createRequestBuilder() {
        return MockMvcRequestBuilders.get(POJO_CONTROLLER_REQUEST_SUFIX);
    }

}
