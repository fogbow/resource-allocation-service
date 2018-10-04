package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.images.Image;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping(value = ImageRequestController.IMAGE_ENDPOINT)
public class ImageRequestController {

    public static final String IMAGE_ENDPOINT = "images";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String MEMBER_ID_HEADER_KEY = "memberId";

    private final Logger LOGGER = Logger.getLogger(ImageRequestController.class);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getAllImages(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId)
            throws Exception {
        LOGGER.info(Messages.Info.RECEIVING_GET_ALL_IMAGES_REQUEST);
        Map<String, String> imagesMap = ApplicationFacade.getInstance().getAllImages(memberId, federationTokenValue);
        return new ResponseEntity<>(imagesMap, HttpStatus.OK);
    }

    @RequestMapping(value = "/{imageId}", method = RequestMethod.GET)
    public ResponseEntity<Image> getImage(@PathVariable String imageId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
            @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_IMAGE_REQUEST, imageId));
        Image image = ApplicationFacade.getInstance().getImage(memberId, imageId, federationTokenValue);
        return new ResponseEntity<>(image, HttpStatus.OK);
    }
}
