package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.ApiDocumentation;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.fogbowcloud.ras.api.http.GenericRequest.GENERIC_REQUEST_ENDPOINT;

@CrossOrigin
@RestController
@RequestMapping(value = GENERIC_REQUEST_ENDPOINT)
public class GenericRequest {

    public static final String GENERIC_REQUEST_ENDPOINT = "genericRequest";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(GenericRequest.class);

    @RequestMapping(method = RequestMethod.POST, value = "/{memberId}" + "/{cloudName}")
    public ResponseEntity<String> genericRequest(@ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
                                                 @PathVariable
                                                         String cloudName,

                                                 @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
                                                 @PathVariable
                                                         String memberId,

                                                 @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
                                                 @RequestHeader(required = true, value = FEDERATION_TOKEN_VALUE_HEADER_KEY)
                                                         String federationTokenValue,

                                                 @RequestBody GenericRequestBean genericRequest)
            throws UnexpectedException, FogbowRasException {

//        FIXME these can be used if we want to get these from the request itself
//        Map<String, String> headers = HttpRequestClientUtil.getHeaders(request);
//        String url = request.getRequestURL().toString();
//        String method = request.getMethod();

        String response = ApplicationFacade.getInstance().genericRequest(
                cloudName, memberId, genericRequest.method, genericRequest.url, genericRequest.headers, genericRequest.body,
                federationTokenValue);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public static class GenericRequestBean {
        private String method;
        private String url;
        private Map<String, String> headers;
        private Map<String, String> body;

        public void setMethod(String method) {
            this.method = method;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public void setBody(Map<String, String> body) {
            this.body = body;
        }
    }
}
