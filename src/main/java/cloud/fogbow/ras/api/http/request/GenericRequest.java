package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
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
    public ResponseEntity<FogbowGenericResponse> genericRequest(
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @RequestBody FogbowGenericRequest genericRequest)
            throws FogbowException {

        FogbowGenericResponse fogbowGenericResponse = ApplicationFacade.getInstance().genericRequest(cloudName,
                memberId, genericRequest, systemUserToken);
        return new ResponseEntity<>(fogbowGenericResponse, HttpStatus.OK);
    }
}
