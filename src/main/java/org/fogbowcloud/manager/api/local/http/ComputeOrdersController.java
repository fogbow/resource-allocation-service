package org.fogbowcloud.manager.api.local.http;

import java.util.List;
import java.util.UUID;

import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value = ComputeOrdersController.COMPUTE_ENDPOINT)
public class ComputeOrdersController {

    public static final String COMPUTE_ENDPOINT = "compute";

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);

    // ExceptionTranslator handles the possible problems in request
    // TODO check if we need to set the value on @RequestHeader
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Order> createCompute(
            @RequestBody ComputeOrder computeOrder,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        LOGGER.info("New compute order request received");

        // The applicationFacade is being loaded here, because the getInstance that was used in the
        // variable declaration
        // disallows a static mock on this method.

        computeOrder.setId(UUID.randomUUID().toString()); // FIXME Find a better location for this
        ApplicationFacade.getInstance().createCompute(computeOrder, federationTokenValue);
        return new ResponseEntity<Order>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ComputeInstance>> getAllCompute(
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        LOGGER.info("Get all compute orders request received");
        List<ComputeInstance> computes = ApplicationFacade.getInstance().getAllComputes(federationTokenValue);
        return new ResponseEntity<>(computes, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ComputeInstance> getCompute(
            @PathVariable String computeId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        LOGGER.info("Get request to compute order with id <" + computeId + "> received");
        ComputeInstance compute = ApplicationFacade.getInstance().getCompute(computeId, federationTokenValue);
        return new ResponseEntity<ComputeInstance>(compute, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCompute(
            @PathVariable String orderId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        LOGGER.info("Get compute order to id <%s> received");
        ApplicationFacade.getInstance().deleteCompute(orderId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
