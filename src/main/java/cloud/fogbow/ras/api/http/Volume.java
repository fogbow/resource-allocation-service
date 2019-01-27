package cloud.fogbow.ras.api.http;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.core.constants.ApiDocumentation;
import cloud.fogbow.ras.core.constants.Messages;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.InstanceStatus;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = Volume.VOLUME_ENDPOINT)
@Api(description = ApiDocumentation.Volume.API)
public class Volume {

    public static final String VOLUME_ENDPOINT = "volumes";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "volume";

    private final Logger LOGGER = Logger.getLogger(Volume.class);

    @ApiOperation(value = ApiDocumentation.Volume.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createVolume(
            @ApiParam(value = ApiDocumentation.Volume.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Volume volume,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String volumeId = ApplicationFacade.getInstance().createVolume(volume.getOrder(), federationTokenValue);
            return new ResponseEntity<String>(volumeId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Volume.GET_OPERATION)
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllVolumesStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws UnavailableProviderException, UnexpectedException, InvalidTokenException,
            UnauthenticatedUserException, UnauthorizedRequestException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> volumeInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.VOLUME);
            return new ResponseEntity<>(volumeInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Volume.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{volumeId}", method = RequestMethod.GET)
    public ResponseEntity<VolumeInstance> getVolume(
            @ApiParam(value = ApiDocumentation.Volume.ID)
            @PathVariable String volumeId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, volumeId));
            VolumeInstance volume = ApplicationFacade.getInstance().getVolume(volumeId, federationTokenValue);
            return new ResponseEntity<>(volume, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Volume.DELETE_OPERATION)
    @RequestMapping(value = "/{volumeId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteVolume(
            @ApiParam(value = ApiDocumentation.Volume.ID)
            @PathVariable String volumeId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, volumeId));
            ApplicationFacade.getInstance().deleteVolume(volumeId, federationTokenValue);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
