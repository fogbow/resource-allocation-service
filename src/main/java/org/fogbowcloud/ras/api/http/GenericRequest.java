package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.ApiDocumentation;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.fogbowcloud.ras.api.http.GenericRequest.GENERIC_REQUEST_ENDPOINT;

@CrossOrigin
@RestController
@RequestMapping(value = GENERIC_REQUEST_ENDPOINT)
public class GenericRequest {

    public static final String GENERIC_REQUEST_ENDPOINT = "genericRequest";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(GenericRequest.class);

    @RequestMapping(method = RequestMethod.POST, value = "/{memberId}" + "/{cloudName}")
    public ResponseEntity<GenericRequestResponse> genericRequest(
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = true, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @RequestBody org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest genericRequest)
            throws FogbowRasException, UnexpectedException {

//        TODO decide which API we're gonna use
//        FIXME these can be used if we want to get these from the request itself
//        Map<String, String> headers = AuditableHttpRequestClient.getHeaders(request);
//        String url = request.getRequestURL().toString();
//        String method = request.getMethod();

        GenericRequestResponse genericRequestResponse = ApplicationFacade.getInstance().genericRequest(cloudName,
                memberId, genericRequest, federationTokenValue);
        return new ResponseEntity<>(genericRequestResponse, HttpStatus.OK);
    }
}
