package org.fogbowcloud.manager.api.local.http;

import java.util.List;

import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
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
@RequestMapping(value = AttachmentOrdersController.VOLUME_LINK_ENDPOINT)
public class AttachmentOrdersController {

    public static final String VOLUME_LINK_ENDPOINT = "volumelink";

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    private final Logger LOGGER = LoggerFactory.getLogger(VolumeOrdersController.class);
    private ApplicationFacade applicationFacade;
    
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Order> createAttachment(@RequestBody AttachmentOrder attachmentOrder,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        LOGGER.info("New volume link request received.");

        this.applicationFacade = ApplicationFacade.getInstance();
        this.applicationFacade.createAttachment(attachmentOrder, federationTokenValue);
        return new ResponseEntity<Order>(HttpStatus.CREATED);
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<AttachmentInstance>> getAllAttachments(
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        List<AttachmentInstance> attachmentInstance = this.applicationFacade.getAllAttachments(federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<AttachmentInstance> getAttachment(@PathVariable String attachmentId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentInstance attachmentInstance = this.applicationFacade.getAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteAttachment(@PathVariable String attachmentId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        this.applicationFacade.deleteAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
}
