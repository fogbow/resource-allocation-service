package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static cloud.fogbow.ras.api.http.request.GenericRequest.GENERIC_REQUEST_ENDPOINT;

@Api(description = ApiDocumentation.GenericRequest.API)
@CrossOrigin
@RestController
@RequestMapping(value = GENERIC_REQUEST_ENDPOINT)
public class GenericRequest {
    public static final String GENERIC_REQUEST_SUFFIX_ENDPOINT = "genericRequest";
    public static final String GENERIC_REQUEST_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + GENERIC_REQUEST_SUFFIX_ENDPOINT;
    public static final String ORDER_CONTROLLER_TYPE = "generic request";

    private final Logger LOGGER = Logger.getLogger(GenericRequest.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @ApiOperation(value = ApiDocumentation.GenericRequest.POST_OPERATION)
    @RequestMapping(method = RequestMethod.POST, value = "/{providerId}" + "/{cloudName}")
    public ResponseEntity<FogbowGenericResponse> genericRequest(
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = true, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @ApiParam(value = ApiDocumentation.GenericRequest.REQUEST_BODY, required = true)
            @RequestBody String genericRequest)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            FogbowGenericResponse fogbowGenericResponse = ApplicationFacade.getInstance().genericRequest(cloudName,
                    providerId, genericRequest, systemUserToken);
            return new ResponseEntity<>(fogbowGenericResponse, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}