package org.fogbowcloud.manager.api.http;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.models.images.Image;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ImageRequestController.IMAGE_ENDPOINT)
public class ImageRequestController {

    public static final String IMAGE_ENDPOINT = "images";

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final String MEMBER_ID_HEADER_KEY = "memberId";

    private final Logger LOGGER = Logger.getLogger(ImageRequestController.class);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getAllImages(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId)
            throws FogbowManagerException {
        LOGGER.info("Get all images request received.");
        Map<String, String> imagesMap = ApplicationFacade.getInstance().getAllImages(memberId, federationTokenValue);
        return new ResponseEntity<>(imagesMap, HttpStatus.OK);
    }

    @RequestMapping(value = "/{imageId}", method = RequestMethod.GET)
    public ResponseEntity<Image> getImage(
            @PathVariable String imageId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId)
            throws FogbowManagerException {
        LOGGER.info("Get image request for <" + imageId + "> received.");
        Image image = ApplicationFacade.getInstance().getImage(memberId, imageId, federationTokenValue);
        return new ResponseEntity<>(image, HttpStatus.OK);
    }
}
