package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.core.ApplicationFacade;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static cloud.fogbow.ras.api.http.request.GenericRequest.GENERIC_REQUEST_ENDPOINT;

@CrossOrigin
@RestController
@RequestMapping(value = GENERIC_REQUEST_ENDPOINT)
public class GenericRequest {

    public static final String GENERIC_REQUEST_ENDPOINT = "genericRequest";

    private final Logger LOGGER = Logger.getLogger(GenericRequest.class);

    @RequestMapping(method = RequestMethod.POST, value = "/{memberId}" + "/{cloudName}")
    public ResponseEntity<GenericRequestResponse> genericRequest(
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @RequestBody cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest genericRequest)
            throws FogbowException {

        GenericRequestResponse genericRequestResponse = ApplicationFacade.getInstance().genericRequest(cloudName,
                memberId, genericRequest, federationTokenValue);
        return new ResponseEntity<>(genericRequestResponse, HttpStatus.OK);
    }
}
