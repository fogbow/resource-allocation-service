package cloud.fogbow.ras.api.http;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.constants.ApiDocumentation;
import cloud.fogbow.ras.core.constants.Messages;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.ApplicationFacade;
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

    private final Logger LOGGER = Logger.getLogger(Image.class);

    @ApiOperation(value = ApiDocumentation.Image.GET_OPERATION)
    @RequestMapping(value = "/{memberId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getAllImages(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_ALL_IMAGES_REQUEST);
            Map<String, String> imagesMap = ApplicationFacade.getInstance().getAllImages(memberId, cloudName, federationTokenValue);
            return new ResponseEntity<>(imagesMap, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Image.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{memberId:.+}" + "/{cloudName}" + "/{imageId}", method = RequestMethod.GET)
    public ResponseEntity<cloud.fogbow.ras.core.models.images.Image> getImage(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.Image.ID)
            @PathVariable String imageId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_IMAGE_REQUEST, imageId));
            cloud.fogbow.ras.core.models.images.Image image = ApplicationFacade.getInstance().getImage(memberId, cloudName, imageId, federationTokenValue);
            return new ResponseEntity<>(image, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
