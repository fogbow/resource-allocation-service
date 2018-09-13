package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = NetworkOrdersController.NETWORK_ENDPOINT)
public class NetworkOrdersController {

    public static final String NETWORK_ENDPOINT = "networks";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "network";

    private final Logger LOGGER = Logger.getLogger(NetworkOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createNetwork(@RequestBody NetworkOrder networkOrder,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info(String.format(Messages.Info.REQUEST_RECEIVED_FOR_NEW_ORDER, ORDER_CONTROLLER_TYPE, networkOrder.getId()));
        String networkId = ApplicationFacade.getInstance().createNetwork(networkOrder, federationTokenValue);
        return new ResponseEntity<String>(networkId, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllNetworksStatus(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.REQUEST_RECEIVED_FOR_GET_ALL_ORDER, ORDER_CONTROLLER_TYPE));
        List<InstanceStatus> networkInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.NETWORK);
        return new ResponseEntity<>(networkInstanceStatus, HttpStatus.OK);
    }

    @RequestMapping(value = "/{networkId}", method = RequestMethod.GET)
    public ResponseEntity<NetworkInstance> getNetwork(@PathVariable String networkId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.REQUEST_RECEIVED_FOR_GET_ORDER, ORDER_CONTROLLER_TYPE, networkId));
        NetworkInstance networkInstance = ApplicationFacade.getInstance().getNetwork(networkId, federationTokenValue);
        return new ResponseEntity<>(networkInstance, HttpStatus.OK);
    }

    @RequestMapping(value = "/{networkId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteNetwork(@PathVariable String networkId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info(String.format(Messages.Info.REQUEST_RECEIVED_FOR_DELETE_ORDER, ORDER_CONTROLLER_TYPE, networkId));
        ApplicationFacade.getInstance().deleteNetwork(networkId, federationTokenValue);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
}
