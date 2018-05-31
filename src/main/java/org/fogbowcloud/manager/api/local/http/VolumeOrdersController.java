package org.fogbowcloud.manager.api.local.http;

import java.util.List;

import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.VolumeInstance;
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
    private final Logger LOGGER = LoggerFactory.getLogger(VolumeOrdersController.class);
    private ApplicationFacade applicationFacade;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Order> createVolume(@RequestBody VolumeOrder volumeOrder,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        LOGGER.info("New volume order request received.");

        this.applicationFacade = ApplicationFacade.getInstance();
        this.applicationFacade.createVolume(volumeOrder, federationTokenValue);
        return new ResponseEntity<Order>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<VolumeInstance>> getAllVolumes(
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        List<VolumeInstance> volumes = this.applicationFacade.getAllVolumes(federationTokenValue);
        return new ResponseEntity<>(volumes, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<VolumeInstance> getVolume(@PathVariable String orderId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        VolumeInstance volume = this.applicationFacade.getVolume(orderId, federationTokenValue);
        return new ResponseEntity<>(volume, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteVolume(@PathVariable String orderId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        this.applicationFacade.deleteVolume(orderId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
