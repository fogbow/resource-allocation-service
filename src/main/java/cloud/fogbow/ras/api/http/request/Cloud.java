package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.CloudList;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.PropertiesHolder;
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
    public static final String CLOUD_SUFFIX_ENDPOINT = "clouds";
    public static final String CLOUD_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + CLOUD_SUFFIX_ENDPOINT;

    private final Logger LOGGER = Logger.getLogger(Cloud.class);

    @ApiOperation(value = ApiDocumentation.Cloud.GET_OPERATION)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<CloudList> getCloudNames(
        @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
        @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
        throws FogbowException {
        try {
            LOGGER.debug(Messages.Log.RECEIVING_GET_CLOUDS_REQUEST);
            String providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
            List<String> cloudNames = ApplicationFacade.getInstance().getCloudNames(providerId, systemUserToken);
            return new ResponseEntity<>(new CloudList(cloudNames), HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Cloud.GET_OPERATION_FOR_PROVIDER)
    @RequestMapping(value = "/{providerId:.+}", method = RequestMethod.GET)
    public ResponseEntity<CloudList> getCloudNames(
            @ApiParam(value = ApiDocumentation.CommonParameters.PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {
        try {
            LOGGER.info(Messages.Log.RECEIVING_GET_CLOUDS_REQUEST);
            List<String> cloudNames = ApplicationFacade.getInstance().getCloudNames(providerId, systemUserToken);
            return new ResponseEntity<>(new CloudList(cloudNames), HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }
}
