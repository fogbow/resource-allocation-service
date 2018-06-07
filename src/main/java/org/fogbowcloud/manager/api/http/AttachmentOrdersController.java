package org.fogbowcloud.manager.api.http;

import java.util.List;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
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
@RequestMapping(value = AttachmentOrdersController.ATTACHMENT_ENDPOINT)
public class AttachmentOrdersController {

    public static final String ATTACHMENT_ENDPOINT = "attachments";

    private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(AttachmentOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createAttachment(@RequestBody AttachmentOrder attachmentOrder,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        LOGGER.info("New attachment request received.");

        String attachmentId = ApplicationFacade.getInstance().createAttachment(attachmentOrder, federationTokenValue);
        return new ResponseEntity<String>(attachmentId, HttpStatus.CREATED);
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<AttachmentInstance>> getAllAttachments(
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        List<AttachmentInstance> attachmentInstance = ApplicationFacade.getInstance().getAllAttachments(federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<AttachmentInstance> getAttachment(@PathVariable String attachmentId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentInstance attachmentInstance = ApplicationFacade.getInstance().getAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteAttachment(@PathVariable String attachmentId,
        @RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        ApplicationFacade.getInstance().deleteAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
}
