package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@CrossOrigin
@RestController
@RequestMapping(value = TokenRequestController.TOKEN_ENDPOINT)
public class TokenRequestController {

    public static final String TOKEN_ENDPOINT = "tokens";

    private final Logger LOGGER = Logger.getLogger(TokenRequestController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createTokenValue(@RequestBody HashMap<String, String> userCredentials)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_TOKEN_REQUEST, userCredentials.size()));
        String tokenValue = ApplicationFacade.getInstance().createTokenValue(userCredentials);
        return new ResponseEntity<String>(tokenValue, HttpStatus.CREATED);
    }
}
