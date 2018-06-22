package org.fogbowcloud.manager.api.http;

import java.util.List;

import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.apache.log4j.Logger;
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

    // ExceptionTranslator handles the possible problems in request
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createCompute(
            @RequestBody ComputeOrder computeOrder,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        LOGGER.info("New compute order request received");

        String computeId = ApplicationFacade.getInstance().createCompute(computeOrder, federationTokenValue);
        return new ResponseEntity<String>(computeId, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ComputeInstance>> getAllCompute(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException,
            UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        LOGGER.info("Get all compute orders request received");
        List<ComputeInstance> computes = ApplicationFacade.getInstance().getAllComputes(federationTokenValue);
	    return ResponseEntity.ok(computes);
    }

    @RequestMapping(value = "/{computeId}", method = RequestMethod.GET)
    public ResponseEntity<ComputeInstance> getCompute(
            @PathVariable String computeId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException,
            UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        LOGGER.info("Get request to compute order with id <" + computeId + "> received");
        ComputeInstance compute = ApplicationFacade.getInstance().getCompute(computeId, federationTokenValue);
        return new ResponseEntity<ComputeInstance>(compute, HttpStatus.OK);
    }

    @RequestMapping(value = "/{computeId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCompute(
            @PathVariable String computeId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        LOGGER.info("Delete compute order to id <" + computeId + "> received");
        ApplicationFacade.getInstance().deleteCompute(computeId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
	@RequestMapping(value = "/quota/{memberId}", method = RequestMethod.GET)
	public ResponseEntity<ComputeQuota> getUserQuota(@PathVariable String memberId,
			@RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws UnauthenticatedException, QuotaException, UnauthorizedException, PropertyNotSpecifiedException,
            RemoteRequestException, TokenCreationException {

		LOGGER.info("User quota information request received.");

		ComputeQuota quotaInstance = ApplicationFacade.getInstance().getComputeQuota(memberId, federationTokenValue);
		return new ResponseEntity<>(quotaInstance, HttpStatus.OK);
	}

	@RequestMapping(value = "/allocation/{memberId}", method = RequestMethod.GET)
	public ResponseEntity<ComputeAllocation> getUserAllocation(@PathVariable String memberId,
			@RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
			throws UnauthenticatedException, QuotaException, UnauthorizedException, RemoteRequestException,
            RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {

		LOGGER.info("User allocation information request received.");

		ComputeAllocation computeAllocation = ApplicationFacade.getInstance().getComputeAllocation(memberId, federationTokenValue);
		return new ResponseEntity<>(computeAllocation, HttpStatus.OK);
	}    
    
}
