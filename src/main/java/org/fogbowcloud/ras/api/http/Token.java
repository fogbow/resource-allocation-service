package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.ApiDocumentation;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@CrossOrigin
@RestController
@RequestMapping(value = Token.TOKEN_ENDPOINT)
@Api(description = ApiDocumentation.Token.API)
public class Token {

    public static final String TOKEN_ENDPOINT = "tokens";

    private final Logger LOGGER = Logger.getLogger(Token.class);

    @ApiOperation(value = ApiDocumentation.Token.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createTokenValue(
            @ApiParam(value = ApiDocumentation.Token.CREATE_REQUEST_BODY)
            @RequestBody HashMap<String, String> userCredentials)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_TOKEN_REQUEST, userCredentials.size()));
            String tokenValue = ApplicationFacade.getInstance().createTokenValue(userCredentials);
            return new ResponseEntity<String>(tokenValue, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
