package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ResourceId;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.ResourceType;
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
@RequestMapping(value = Volume.VOLUME_ENDPOINT)
@Api(description = ApiDocumentation.Volume.API)
public class Volume {

    public static final String VOLUME_ENDPOINT = "volumes";
    public static final String ORDER_CONTROLLER_TYPE = "volume";

    private final Logger LOGGER = Logger.getLogger(Volume.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @ApiOperation(value = ApiDocumentation.Volume.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createVolume(
            @ApiParam(value = ApiDocumentation.Volume.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Volume volume,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String volumeId = ApplicationFacade.getInstance().createVolume(volume.getOrder(), systemUserToken);
            return new ResponseEntity<>(new ResourceId(volumeId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Volume.GET_OPERATION)
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllVolumesStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> volumeInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(systemUserToken, ResourceType.VOLUME);
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
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, volumeId));
            VolumeInstance volume = ApplicationFacade.getInstance().getVolume(volumeId, systemUserToken);
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
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, volumeId));
            ApplicationFacade.getInstance().deleteVolume(volumeId, systemUserToken);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
