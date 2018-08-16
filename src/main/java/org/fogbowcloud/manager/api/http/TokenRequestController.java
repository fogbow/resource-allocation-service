package org.fogbowcloud.manager.api.http;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = TokenRequestController.TOKEN_ENDPOINT)
public class TokenRequestController {

    public static final String TOKEN_ENDPOINT = "tokens";

    private final Logger LOGGER = Logger.getLogger(TokenRequestController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createTokenValue(@RequestBody HashMap<String, String> userCredentials)
            throws FogbowManagerException, UnexpectedException {
        String tokenValue = ApplicationFacade.getInstance().createTokenValue(userCredentials);
        return new ResponseEntity<String>(tokenValue, HttpStatus.CREATED);
    }
}
