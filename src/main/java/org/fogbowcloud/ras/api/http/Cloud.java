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

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = Cloud.CLOUD_ENDPOINT)
@Api(description = ApiDocumentation.Cloud.API)
public class Cloud {

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String MEMBER_ID_HEADER_KEY = "memberId";
    public static final String CLOUD_ENDPOINT = "clouds";

    private final Logger LOGGER = Logger.getLogger(Cloud.class);

    @ApiOperation(value = ApiDocumentation.Cloud.GET_OPERATION)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<String>> getCloudNames(
        @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue,
        @ApiParam(value = ApiDocumentation.CommonParameters.MEMBER_ID)
        @RequestHeader(required = false, value = MEMBER_ID_HEADER_KEY) String memberId) throws Exception {
        try {
            LOGGER.info(Messages.Info.RECEIVING_GET_CLOUDS_REQUEST);
            List<String> cloudNames = ApplicationFacade.getInstance().getCloudNames(memberId, federationTokenValue);
            return new ResponseEntity<>(cloudNames, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }
}
