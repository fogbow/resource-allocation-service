package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.ApiDocumentation;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = Compute.COMPUTE_ENDPOINT)
@Api(description = ApiDocumentation.Compute.API)
public class Compute {

    public static final String COMPUTE_ENDPOINT = "computes";
    public static final String STATUS_ENDPOINT = "status";
    public static final String QUOTA_ENDPOINT = "quota";
    public static final String ALLOCATION_ENDPOINT = "allocation";
    public static final String CLOUD_NAME_HEADER_KEY = "cloudName";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "compute";

    private final Logger LOGGER = Logger.getLogger(Compute.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request
    @ApiOperation(value = ApiDocumentation.Compute.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createCompute(
            @ApiParam(value = ApiDocumentation.Compute.CREATE_REQUEST_BODY)
            @RequestBody org.fogbowcloud.ras.api.parameters.Compute compute,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));

            ComputeOrder computeOrder = compute.getOrder();
            // if userData is null we need to prevent a NullPointerException when trying to save the order
            // in the database
            if (computeOrder.getUserData() == null) {
                computeOrder.setUserData(new ArrayList<>());
            }

            String computeId = ApplicationFacade.getInstance().createCompute(computeOrder, federationTokenValue);
            return new ResponseEntity<String>(computeId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_OPERATION)
    @RequestMapping(value = "/" + STATUS_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllComputesStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> computeInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.COMPUTE);
            return new ResponseEntity<>(computeInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{computeId}", method = RequestMethod.GET)
    public ResponseEntity<ComputeInstance> getCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, computeId));
            ComputeInstance compute = ApplicationFacade.getInstance().getCompute(computeId, federationTokenValue);
            return new ResponseEntity<ComputeInstance>(compute, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.DELETE_OPERATION)
    @RequestMapping(value = "/{computeId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCompute(
            @ApiParam(value = ApiDocumentation.Compute.ID)
            @PathVariable String computeId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, computeId));
            ApplicationFacade.getInstance().deleteCompute(computeId, federationTokenValue);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_QUOTA)
    @RequestMapping(value = "/" + QUOTA_ENDPOINT + "/{memberId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<ComputeQuota> getUserQuota(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_COMPUTE_QUOTA_REQUEST, QUOTA_ENDPOINT, memberId));
            ComputeQuota quotaInstance = ApplicationFacade.getInstance().getComputeQuota(memberId, cloudName, federationTokenValue);
            return new ResponseEntity<>(quotaInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Compute.GET_ALLOCATION)
    @RequestMapping(value = "/" + ALLOCATION_ENDPOINT + "/{memberId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<ComputeAllocation> getUserAllocation(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_COMPUTE_QUOTA_REQUEST, ALLOCATION_ENDPOINT, memberId));
            ComputeAllocation computeAllocation =
                ApplicationFacade.getInstance().getComputeAllocation(memberId, cloudName, federationTokenValue);
            return new ResponseEntity<>(computeAllocation, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
