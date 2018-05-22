package org.fogbowcloud.manager.api.local.http;

import java.util.List;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
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

    private ApplicationFacade applicationFacade;
    private final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);

    // ExceptionTranslator handles the possible problems in request
    // TODO check if we need to set the value on @RequestHeader
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Order> createCompute(
            @RequestBody ComputeOrder computeOrder,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws UnauthenticatedException, Exception {
        LOGGER.info("New compute order request received");

        // The applicationFacade is being loaded here, because the getInstance that was used in the
        // variable declaration
        // disallows a static mock on this method.
        this.applicationFacade = ApplicationFacade.getInstance();
        this.applicationFacade.createCompute(computeOrder, federationTokenValue);
        return new ResponseEntity<Order>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ComputeOrder>> getAllCompute(
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get all compute orders request received");
        List<ComputeOrder> orders = this.applicationFacade.getAllComputes(federationTokenValue);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ComputeOrder> getCompute(
            @PathVariable String computeId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get request to compute order with id <" + computeId + "> received");
        ComputeOrder order = this.applicationFacade.getCompute(computeId, federationTokenValue);
        return new ResponseEntity<ComputeOrder>(order, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCompute(
            @PathVariable String computeId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get compute order to id <%s> received");
        this.applicationFacade.deleteCompute(computeId, federationTokenValue);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
}
