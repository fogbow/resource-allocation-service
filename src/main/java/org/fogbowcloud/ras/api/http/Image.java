package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.ApiDocumentation;
import org.fogbowcloud.ras.core.constants.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping(value = Image.IMAGE_ENDPOINT)
@Api(description = ApiDocumentation.Image.API)
public class Image {

    public static final String IMAGE_ENDPOINT = "images";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String MEMBER_ID_HEADER_KEY = "memberId";
    public static final String CLOUD_NAME_HEADER_KEY = "cloudName";

    private final Logger LOGGER = Logger.getLogger(Image.class);

    @ApiOperation(value = ApiDocumentation.Image.GET_OPERATION)
    @RequestMapping(value = "/{memberId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getAllImages(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_ALL_IMAGES_REQUEST);
            Map<String, String> imagesMap = ApplicationFacade.getInstance().getAllImages(memberId, cloudName, federationTokenValue);
            return new ResponseEntity<>(imagesMap, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Image.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{memberId:.+}" + "/{cloudName}" + "/{imageId}", method = RequestMethod.GET)
    public ResponseEntity<org.fogbowcloud.ras.core.models.images.Image> getImage(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.Image.ID)
            @PathVariable String imageId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_IMAGE_REQUEST, imageId));
            org.fogbowcloud.ras.core.models.images.Image image = ApplicationFacade.getInstance().getImage(memberId, cloudName, imageId, federationTokenValue);
            return new ResponseEntity<>(image, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Image.GET_OPERATION_DEPRECATED)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getAllImagesDeprecated(
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @RequestHeader(required = false, value = CLOUD_NAME_HEADER_KEY) String cloudName)
            throws Exception {

        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_ALL_IMAGES_REQUEST);
            Map<String, String> imagesMap = ApplicationFacade.getInstance().getAllImages(memberId, cloudName, federationTokenValue);
            return new ResponseEntity<>(imagesMap, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Image.GET_BY_ID_OPERATION_DEPRECATED)
    @RequestMapping(value = "/{imageId}", method = RequestMethod.GET)
    public ResponseEntity<org.fogbowcloud.ras.core.models.images.Image> getImageDeprecated(
            @ApiParam(value = ApiDocumentation.Image.ID)
            @PathVariable String imageId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @RequestHeader(required = false, value = CLOUD_NAME_HEADER_KEY) String cloudName)
            throws Exception {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_IMAGE_REQUEST, imageId));
            org.fogbowcloud.ras.core.models.images.Image image = ApplicationFacade.getInstance().getImage(memberId, cloudName, imageId, federationTokenValue);
            return new ResponseEntity<>(image, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }
}
