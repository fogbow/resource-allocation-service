package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.ApiDocumentation;
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
@Api(description = ApiDocumentation.Attachment.API)
public class Attachment {

    public static final String ATTACHMENT_ENDPOINT = "attachments";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "attachment";

    private final Logger LOGGER = Logger.getLogger(Attachment.class);

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = ApiDocumentation.Attachment.CREATE_OPERATION)
    public ResponseEntity<String> createAttachment(
            @ApiParam(value = ApiDocumentation.Attachment.CREATE_REQUEST_BODY)
            @RequestBody org.fogbowcloud.ras.api.parameters.Attachment attachment,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String attachmentId = ApplicationFacade.getInstance().createAttachment(attachment.getOrder(), federationTokenValue);
            return new ResponseEntity<String>(attachmentId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Attachment.GET_OPERATION)
    public ResponseEntity<List<InstanceStatus>> getAllAttachmentsStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> attachmentInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.ATTACHMENT);
            return new ResponseEntity<>(attachmentInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Attachment.GET_BY_ID_OPERATION)
    public ResponseEntity<AttachmentInstance> getAttachment(
            @ApiParam(value = ApiDocumentation.Attachment.ID)
            @PathVariable String attachmentId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, attachmentId));
            AttachmentInstance attachmentInstance =
                ApplicationFacade.getInstance().getAttachment(attachmentId, federationTokenValue);
            return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.DELETE)
    @ApiOperation(value = ApiDocumentation.Attachment.DELETE_OPERATION)
    public ResponseEntity<Boolean> deleteAttachment(
            @ApiParam(value = ApiDocumentation.Attachment.ID)
            @PathVariable String attachmentId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, attachmentId));
            ApplicationFacade.getInstance().deleteAttachment(attachmentId, federationTokenValue);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }
}
