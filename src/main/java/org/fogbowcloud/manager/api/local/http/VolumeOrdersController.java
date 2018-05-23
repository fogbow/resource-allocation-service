package org.fogbowcloud.manager.api.local.http;

import java.util.List;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
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
@RequestMapping(value = VolumeOrdersController.VOLUME_ENDPOINT)
public class VolumeOrdersController {

    public static final String VOLUME_ENDPOINT = "volume";

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private ApplicationFacade applicationFacade;
    private final Logger LOGGER = LoggerFactory.getLogger(VolumeOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Order> createVolume(@RequestBody VolumeOrder volumeOrder,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws UnauthenticatedException, Exception {
        LOGGER.info("New volume order request received.");

        this.applicationFacade = ApplicationFacade.getInstance();
        this.applicationFacade.createVolume(volumeOrder, federationTokenValue);
        return new ResponseEntity<Order>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<VolumeOrder>> getAllVolumes(
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        List<VolumeOrder> orders = this.applicationFacade.getAllVolumes(federationTokenValue);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<VolumeOrder> getVolume(@PathVariable String volumeId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        VolumeOrder order = this.applicationFacade.getVolume(volumeId, federationTokenValue);
        return new ResponseEntity<VolumeOrder>(order, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteVolume(@PathVariable String volumeId,
            @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        this.applicationFacade.deleteVolume(volumeId, federationTokenValue);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }

}
