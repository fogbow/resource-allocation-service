package org.fogbowcloud.manager.api.local.http;

import java.util.List;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "network")
public class NetworkOrdersController {

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private ApplicationFacade applicationFacade;
    private final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);
    
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Order> createNetwork(@RequestBody NetworkOrder networkOrder,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws UnauthenticatedException, Exception {
        LOGGER.info("New volume order request received.");

        this.applicationFacade = ApplicationFacade.getInstance();
        this.applicationFacade.createNetwork(networkOrder, federationTokenValue);
        return new ResponseEntity<Order>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<NetworkOrder>> getAllNetworks(
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        List<NetworkOrder> orders = this.applicationFacade.getAllNetworks(federationTokenValue);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<NetworkOrder> getNetwork(@PathVariable String networkId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        NetworkOrder order = this.applicationFacade.getNetwork(networkId, federationTokenValue);
        return new ResponseEntity<NetworkOrder>(order, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteNetwork(@PathVariable String networkId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        this.applicationFacade.deleteNetwork(networkId, federationTokenValue);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
    
}
