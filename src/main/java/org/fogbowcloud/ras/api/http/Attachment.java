package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = Attachment.ATTACHMENT_ENDPOINT)
@Api(description = "Manage attachment resources")
public class Attachment {

    public static final String ATTACHMENT_ENDPOINT = "attachments";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "attachment";

    private final Logger LOGGER = Logger.getLogger(Attachment.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createAttachment(@RequestBody org.fogbowcloud.ras.api.parameters.Attachment attachment,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
        String attachmentId = ApplicationFacade.getInstance().createAttachment(attachment.getOrder(), federationTokenValue);
        return new ResponseEntity<String>(attachmentId, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllAttachmentsStatus(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
        List<InstanceStatus> attachmentInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.ATTACHMENT);
        return new ResponseEntity<>(attachmentInstanceStatus, HttpStatus.OK);
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.GET)
    public ResponseEntity<AttachmentInstance> getAttachment(@PathVariable String attachmentId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, attachmentId));
        AttachmentInstance attachmentInstance =
                ApplicationFacade.getInstance().getAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteAttachment(@PathVariable String attachmentId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, attachmentId));
        ApplicationFacade.getInstance().deleteAttachment(attachmentId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
