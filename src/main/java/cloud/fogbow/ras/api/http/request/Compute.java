package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ResourceId;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
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
    public static final String QUOTA_SUFFIX_ENDPOINT = "quota";
    public static final String ALLOCATION_SUFFIX_ENDPOINT = "allocation";
    public static final String COMPUTE_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + COMPUTE_SUFFIX_ENDPOINT;
    public static final String ORDER_CONTROLLER_TYPE = "compute";

    private final Logger LOGGER = Logger.getLogger(Compute.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @ApiOperation(value = ApiDocumentation.Compute.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createCompute(
            @ApiParam(value = ApiDocumentation.Compute.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Compute compute,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String computeId = ApplicationFacade.getInstance().createCompute(compute.getOrder(), systemUserToken);
            return new ResponseEntity<>(new ResourceId(computeId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
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
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
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
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, computeId));
            ComputeInstance compute = ApplicationFacade.getInstance().getCompute(computeId, systemUserToken);
            return new ResponseEntity<ComputeInstance>(compute, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
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
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, computeId));
            ApplicationFacade.getInstance().deleteCompute(computeId, systemUserToken);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
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
            LOGGER.info(String.format(Messages.Info.RECEIVING_COMPUTE_QUOTA_REQUEST, ALLOCATION_SUFFIX_ENDPOINT, providerId));
            ComputeAllocation computeAllocation =
                ApplicationFacade.getInstance().getComputeAllocation(providerId, cloudName, systemUserToken);
            return new ResponseEntity<>(computeAllocation, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
