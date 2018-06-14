package org.fogbowcloud.manager.api.http;

import java.util.List;

import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.apache.log4j.Logger;
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

    public static final String VOLUME_ENDPOINT = "volumes";

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(VolumeOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createVolume(@RequestBody VolumeOrder volumeOrder,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        LOGGER.info("New volume order request received.");

        String volumeId = ApplicationFacade.getInstance().createVolume(volumeOrder, federationTokenValue);
        return new ResponseEntity<String>(volumeId, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<VolumeInstance>> getAllVolumes(
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        List<VolumeInstance> volumes = ApplicationFacade.getInstance().getAllVolumes(federationTokenValue);
        return new ResponseEntity<>(volumes, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<VolumeInstance> getVolume(@PathVariable String orderId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        VolumeInstance volume = ApplicationFacade.getInstance().getVolume(orderId, federationTokenValue);
        return new ResponseEntity<>(volume, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteVolume(@PathVariable String orderId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        ApplicationFacade.getInstance().deleteVolume(orderId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
