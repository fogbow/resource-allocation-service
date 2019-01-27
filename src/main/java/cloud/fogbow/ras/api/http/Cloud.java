package cloud.fogbow.ras.api.http;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ApiDocumentation;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = Cloud.CLOUD_ENDPOINT)
@Api(description = ApiDocumentation.Cloud.API)
public class Cloud {

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String CLOUD_ENDPOINT = "clouds";

    private final Logger LOGGER = Logger.getLogger(Cloud.class);

    @ApiOperation(value = ApiDocumentation.Cloud.GET_OPERATION)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<String>> getCloudNamesNoMemberId(
        @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws FogbowException {
        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_CLOUDS_REQUEST);
            String memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
            List<String> cloudNames = ApplicationFacade.getInstance().getCloudNames(memberId, federationTokenValue);
            return new ResponseEntity<>(cloudNames, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Cloud.GET_OPERATION_FOR_MEMBER)
    @RequestMapping(value = "/{memberId:.+}", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getCloudNames(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {
        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_CLOUDS_REQUEST);
            List<String> cloudNames = ApplicationFacade.getInstance().getCloudNames(memberId, federationTokenValue);
            return new ResponseEntity<>(cloudNames, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
