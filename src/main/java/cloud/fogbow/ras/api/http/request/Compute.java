package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ResourceId;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.parameters.Snapshot;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
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
@RequestMapping(value = Compute.COMPUTE_ENDPOINT)
@Api(description = ApiDocumentation.Compute.API)
public class Compute {
    public static final String COMPUTE_SUFFIX_ENDPOINT = "computes";
    public static final String STATUS_SUFFIX_ENDPOINT = "status";
    public static final String ALLOCATION_SUFFIX_ENDPOINT = "allocation";
    public static final String COMPUTE_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + COMPUTE_SUFFIX_ENDPOINT;
    public static final String ORDER_CONTROLLER_TYPE = "compute";
    public static final String PAUSE_COMPUTE_ENDPOINT = COMPUTE_ENDPOINT + "/pause";
    public static final String HIBERNATE_COMPUTE_ENDPOINT = COMPUTE_ENDPOINT + "/hibernate";
    public static final String STOP_COMPUTE_ENDPOINT = COMPUTE_ENDPOINT + "/stop";
    public static final String RESUME_COMPUTE_ENDPOINT = COMPUTE_ENDPOINT + "/resume";
    
    private final Logger LOGGER = Logger.getLogger(Compute.class);

    @RequestMapping(value = "/{computeId}/snapshot", method = RequestMethod.POST)
    public ResponseEntity<Boolean> takeSnapshot(
            @PathVariable String computeId,
            @RequestBody Snapshot snapshot,
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException{
        try {
            ApplicationFacade.getInstance().takeSnapshot(computeId, snapshot.getName(), systemUserToken, ResourceType.COMPUTE);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ApiOperation(value = ApiDocumentation.Compute.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createCompute(
            @ApiParam(value = ApiDocumentation.Compute.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Compute compute,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_CREATE_REQUEST_S, ORDER_CONTROLLER_TYPE));
            String computeId = ApplicationFacade.getInstance().createCompute(compute.getOrder(), systemUserToken);
            return new ResponseEntity<>(new ResourceId(computeId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Log.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_OPERATION)
    @RequestMapping(value = "/" + STATUS_SUFFIX_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllComputesStatus(
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {
        try {
            List<InstanceStatus> computeInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(systemUserToken, ResourceType.COMPUTE);
            return new ResponseEntity<>(computeInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{computeId}", method = RequestMethod.GET)
    public ResponseEntity<ComputeInstance> getCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_GET_REQUEST_S, ORDER_CONTROLLER_TYPE, computeId));
            ComputeInstance compute = ApplicationFacade.getInstance().getCompute(computeId, systemUserToken);
            return new ResponseEntity<ComputeInstance>(compute, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.DELETE_OPERATION)
    @RequestMapping(value = "/{computeId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_DELETE_REQUEST_S_S, ORDER_CONTROLLER_TYPE, computeId));
            ApplicationFacade.getInstance().deleteCompute(computeId, systemUserToken);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_ALLOCATION)
    @RequestMapping(value = "/" + ALLOCATION_SUFFIX_ENDPOINT + "/{providerId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<ComputeAllocation> getUserAllocation(
            @ApiParam(value = ApiDocumentation.CommonParameters.PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_COMPUTE_QUOTA_REQUEST_S_S, ALLOCATION_SUFFIX_ENDPOINT, providerId));
            ComputeAllocation computeAllocation =
                ApplicationFacade.getInstance().getComputeAllocation(providerId, cloudName, systemUserToken);
            return new ResponseEntity<>(computeAllocation, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.PAUSE_OPERATION)
    @RequestMapping(value = "/{computeId}/pause", method = RequestMethod.POST)
    public void pauseCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_PAUSE_REQUEST_S, computeId));
            ApplicationFacade.getInstance().pauseCompute(computeId, systemUserToken, ResourceType.COMPUTE);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.PAUSE_USER_OPERATION)
    @RequestMapping(value = "/pause/{userId}/{providerId:.+}", method = RequestMethod.POST)
    public void pauseUserComputes(
            @ApiParam(value = ApiDocumentation.Compute.USER_ID)
            @PathVariable String userId,
            @ApiParam(value = ApiDocumentation.Compute.USER_PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_PAUSE_USER_REQUEST_S, userId, providerId));
            ApplicationFacade.getInstance().pauseUserComputes(userId, providerId, systemUserToken);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.HIBERNATE_OPERATION)
    @RequestMapping(value = "/{computeId}/hibernate", method = RequestMethod.POST)
    public void hibernateCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_GET_REQUEST_S, ORDER_CONTROLLER_TYPE, computeId));
            ApplicationFacade.getInstance().hibernateCompute(computeId, systemUserToken, ResourceType.COMPUTE);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }
    
    @ApiOperation(value = ApiDocumentation.Compute.HIBERNATE_OPERATION)
    @RequestMapping(value = "/hibernate/{userId}/{providerId:.+}", method = RequestMethod.POST)
    public void hibernateUserComputes(
            @ApiParam(value = ApiDocumentation.Compute.USER_ID)
            @PathVariable String userId,
            @ApiParam(value = ApiDocumentation.Compute.USER_PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_HIBERNATE_USER_REQUEST_S, userId, providerId));
            ApplicationFacade.getInstance().hibernateUserComputes(userId, providerId, systemUserToken);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }
    
    @ApiOperation(value = ApiDocumentation.Compute.STOP_OPERATION)
    @RequestMapping(value = "/{computeId}/stop", method = RequestMethod.POST)
    public void stopCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) 
                    throws FogbowException {
        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_STOP_REQUEST_S, computeId));
            ApplicationFacade.getInstance().stopCompute(computeId, systemUserToken, ResourceType.COMPUTE);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }
    
    @ApiOperation(value = ApiDocumentation.Compute.STOP_USER_OPERATION)
    @RequestMapping(value = "/stop/{userId}/{providerId:.+}", method = RequestMethod.POST)
    public void stopUserComputes(
            @ApiParam(value = ApiDocumentation.Compute.USER_ID)
            @PathVariable String userId,
            @ApiParam(value = ApiDocumentation.Compute.USER_PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) 
                    throws FogbowException {
        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_STOP_USER_REQUEST_S, userId, providerId));
            ApplicationFacade.getInstance().stopUserComputes(userId, providerId, systemUserToken);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.RESUME_OPERATION)
    @RequestMapping(value = "/{computeId}/resume", method = RequestMethod.POST)
    public void resumeCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_RESUME_REQUEST_S, computeId));
            ApplicationFacade.getInstance().resumeCompute(computeId, systemUserToken, ResourceType.COMPUTE);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.RESUME_USER_OPERATION)
    @RequestMapping(value = "/resume/{userId}/{providerId:.+}", method = RequestMethod.POST)
    public void resumeUserComputes(
            @ApiParam(value = ApiDocumentation.Compute.USER_ID)
            @PathVariable String userId,
            @ApiParam(value = ApiDocumentation.Compute.USER_PROVIDER_ID)
            @PathVariable String providerId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Log.RECEIVING_RESUME_USER_REQUEST_S, userId, providerId));
            ApplicationFacade.getInstance().resumeUserComputes(userId, providerId, systemUserToken);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()), e);
            throw e;
        }
    }
}
