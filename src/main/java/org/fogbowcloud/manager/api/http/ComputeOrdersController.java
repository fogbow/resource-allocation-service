package org.fogbowcloud.manager.api.http;

import java.util.List;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = ComputeOrdersController.COMPUTE_ENDPOINT)
public class ComputeOrdersController {

    public static final String COMPUTE_ENDPOINT = "computes";

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(ComputeOrdersController.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createCompute(
            @RequestBody ComputeOrder computeOrder,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("New compute order request received <" + computeOrder.getId() + ">.");
        String computeId = ApplicationFacade.getInstance().createCompute(computeOrder, federationTokenValue);
        return new ResponseEntity<String>(computeId, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ComputeInstance>> getAllComputes(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get all compute order requests received.");
        List<ComputeInstance> computes = ApplicationFacade.getInstance().getAllComputes(federationTokenValue);
        return new ResponseEntity<>(computes, HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllComputesStatus(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get the status of all compute order requests received.");
        List<InstanceStatus> computeInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, InstanceType.COMPUTE);
        return new ResponseEntity<>(computeInstanceStatus, HttpStatus.OK);
    }

    @RequestMapping(value = "/{computeId}", method = RequestMethod.GET)
    public ResponseEntity<ComputeInstance> getCompute(
            @PathVariable String computeId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get request for compute order <" + computeId + "> received.");
        ComputeInstance compute = ApplicationFacade.getInstance().getCompute(computeId, federationTokenValue);
        return new ResponseEntity<ComputeInstance>(compute, HttpStatus.OK);
    }

    @RequestMapping(value = "/{computeId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCompute(
            @PathVariable String computeId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("Delete compute order <" + computeId + "> received.");
        ApplicationFacade.getInstance().deleteCompute(computeId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
	@RequestMapping(value = "/quota/{memberId:.+}", method = RequestMethod.GET)
	public ResponseEntity<ComputeQuota> getUserQuota(@PathVariable String memberId,
			@RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

		LOGGER.info("User quota information request for member <" + memberId + "> received.");
		ComputeQuota quotaInstance = ApplicationFacade.getInstance().getComputeQuota(memberId, federationTokenValue);
		return new ResponseEntity<>(quotaInstance, HttpStatus.OK);
	}

	@RequestMapping(value = "/allocation/{memberId:.+}", method = RequestMethod.GET)
	public ResponseEntity<ComputeAllocation> getUserAllocation(@PathVariable String memberId,
			@RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowManagerException, UnexpectedException {

		LOGGER.info("User allocation information request for member <" + memberId + "> received.");
		ComputeAllocation computeAllocation =
                ApplicationFacade.getInstance().getComputeAllocation(memberId, federationTokenValue);
		return new ResponseEntity<>(computeAllocation, HttpStatus.OK);
	}    
    
}
