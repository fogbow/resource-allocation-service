package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping(value = Image.IMAGE_ENDPOINT)
@Api(description = ApiDocumentation.Image.API)
public class Image {
    public static final String IMAGE_SUFFIX_ENDPOINT = "images";
    public static final String IMAGE_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + IMAGE_SUFFIX_ENDPOINT;

    private final Logger LOGGER = Logger.getLogger(Image.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @ApiOperation(value = ApiDocumentation.Image.GET_OPERATION)
    @RequestMapping(value = "/{memberId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getAllImages(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_ALL_IMAGES_REQUEST);
            Map<String, String> imagesMap = ApplicationFacade.getInstance().getAllImages(memberId, cloudName, systemUserToken);
            return new ResponseEntity<>(imagesMap, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Image.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{memberId:.+}" + "/{cloudName}" + "/{imageId}", method = RequestMethod.GET)
    public ResponseEntity<ImageInstance> getImage(
            @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
            @PathVariable String memberId,
            @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
            @PathVariable String cloudName,
            @ApiParam(value = ApiDocumentation.Image.ID)
            @PathVariable String imageId,
            @ApiParam(value = ApiDocumentation.CommonParameters.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_IMAGE_REQUEST, imageId));
            ImageInstance imageInstance = ApplicationFacade.getInstance().getImage(memberId, cloudName, imageId, systemUserToken);
            return new ResponseEntity<>(imageInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
