package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ResourceId;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.ResourceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = Attachment.ATTACHMENT_ENDPOINT)
@Api(description = ApiDocumentation.Attachment.API)
public class Attachment {
    public static final String ATTACHMENT_SUFFIX_ENDPOINT = "attachments";
    public static final String ATTACHMENT_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + ATTACHMENT_SUFFIX_ENDPOINT;
    public static final String ORDER_CONTROLLER_TYPE = "attachment";

    private final Logger LOGGER = Logger.getLogger(Attachment.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = ApiDocumentation.Attachment.CREATE_OPERATION)
    public ResponseEntity<ResourceId> createAttachment(
            @ApiParam(value = ApiDocumentation.Attachment.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Attachment attachment,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String attachmentId = ApplicationFacade.getInstance().createAttachment(attachment.getOrder(), systemUserToken);
            return new ResponseEntity<>(new ResourceId(attachmentId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Attachment.GET_OPERATION)
    public ResponseEntity<List<InstanceStatus>> getAllAttachmentsStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> attachmentInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(systemUserToken, ResourceType.ATTACHMENT);
            return new ResponseEntity<>(attachmentInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.GET)
    @ApiOperation(value = ApiDocumentation.Attachment.GET_BY_ID_OPERATION)
    public ResponseEntity<AttachmentInstance> getAttachment(
            @ApiParam(value = ApiDocumentation.Attachment.ID)
            @PathVariable String attachmentId,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, attachmentId));
            AttachmentInstance attachmentInstance =
                ApplicationFacade.getInstance().getAttachment(attachmentId, systemUserToken);
            return new ResponseEntity<>(attachmentInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @RequestMapping(value = "/{attachmentId}", method = RequestMethod.DELETE)
    @ApiOperation(value = ApiDocumentation.Attachment.DELETE_OPERATION)
    public ResponseEntity<Boolean> deleteAttachment(
            @ApiParam(value = ApiDocumentation.Attachment.ID)
            @PathVariable String attachmentId,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, attachmentId));
            ApplicationFacade.getInstance().deleteAttachment(attachmentId, systemUserToken);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
