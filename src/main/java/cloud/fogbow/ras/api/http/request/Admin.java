package cloud.fogbow.ras.api.http.request;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.parameters.Policy;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOrigin
@RestController
@RequestMapping(value = Admin.ADMIN_ENDPOINT)
@Api(description = ApiDocumentation.Admin.API)
public class Admin {
    public static final String ADMIN_SUFFIX_ENDPOINT = "admin";
    public static final String ADMIN_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + ADMIN_SUFFIX_ENDPOINT;
    public static final String PURGE_USER_ENDPOINT = ADMIN_ENDPOINT + "/purge";
    
    private final Logger LOGGER = Logger.getLogger(Admin.class);
    
    @ApiOperation(value = ApiDocumentation.Admin.RELOAD_OPERATION)
    @RequestMapping(value = "/reload", method = RequestMethod.POST)
    public ResponseEntity<Boolean> reload(
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        LOGGER.info(Messages.Log.RECEIVING_RELOAD_CONFIGURATION_REQUEST);
        ApplicationFacade.getInstance().reload(systemUserToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.SET_POLICY_OPERATION)
    @RequestMapping(value = "/policy", method = RequestMethod.POST)
    public ResponseEntity<Boolean> setPolicy(
    		@RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken, 
    		@ApiParam(value = ApiDocumentation.Admin.SET_POLICY_REQUEST_BODY)
    		@RequestBody Policy policy) throws FogbowException {
    	ApplicationFacade.getInstance().setPolicy(systemUserToken, policy.getPolicy());
    	return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.UPDATE_POLICY_OPERATION)
    @RequestMapping(value = "/policy", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> updatePolicy(
    		@RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
            @ApiParam(value = ApiDocumentation.Admin.UPDATE_POLICY_REQUEST_BODY)
    		@RequestBody Policy policy) throws FogbowException {
    	ApplicationFacade.getInstance().updatePolicy(systemUserToken, policy.getPolicy());
    	return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @ApiOperation(value = ApiDocumentation.Admin.PURGE_USER_OPERATION)
    @RequestMapping(value = "/purge/{userId}/{provider:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> purgeUser(
            @ApiParam(value = ApiDocumentation.Admin.USER_ID)
            @PathVariable String userId, 
            @ApiParam(value = ApiDocumentation.Admin.USER_PROVIDER_ID)
            @PathVariable String provider,
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) 
                    throws FogbowException {
        ApplicationFacade.getInstance().purgeUser(systemUserToken, userId, provider);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
