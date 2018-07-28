package org.fogbowcloud.manager.api.http;

import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
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

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(AttachmentOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createAttachment(@RequestBody AttachmentOrder attachmentOrder,
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("New attachment order request received <" + attachmentOrder.getId() + ">.");

        String attachmentId = ApplicationFacade.getInstance().createAttachment(attachmentOrder, federationTokenValue);
        return new ResponseEntity<String>(attachmentId, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<AttachmentInstance>> getAllAttachments(
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get all attachment order requests received.");
        List<AttachmentInstance> attachmentInstance =
                ApplicationFacade.getInstance().getAllAttachments(federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllAttachmentsStatus(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get the status of all attachment order requests received.");
        List<InstanceStatus> attachmentInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.ATTACHMENT);
        return new ResponseEntity<>(attachmentInstanceStatus, HttpStatus.OK);
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.GET)
    public ResponseEntity<AttachmentInstance> getAttachment(@PathVariable String attachmentId,
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get request for attachment order <" + attachmentId + "> received.");
        AttachmentInstance attachmentInstance =
                ApplicationFacade.getInstance().getAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteAttachment(@PathVariable String attachmentId,
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Delete attachment order <" + attachmentId + "> received.");
        ApplicationFacade.getInstance().deleteAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
