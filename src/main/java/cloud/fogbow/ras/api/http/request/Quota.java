package cloud.fogbow.ras.api.http.request;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOrigin
@RestController
@RequestMapping(value = Quota.QUOTA_ENDPOINT)
@Api(ApiDocumentation.Quota.API)
public class Quota {

    private static final Logger LOGGER = Logger.getLogger(Quota.class);

    public static final String QUOTA_SUFFIX_ENDPOINT = "quota";
    public static final String QUOTA_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + QUOTA_SUFFIX_ENDPOINT;

    @ApiOperation(value = ApiDocumentation.Quota.GET_QUOTA)
    @RequestMapping(value = "/{providerId:.+}" + "/{cloudName}", method = RequestMethod.GET)
    public ResponseEntity<ResourceQuota> getAllQuota(
        @ApiParam(value = ApiDocumentation.CommonParameters.PROVIDER_ID)
        @PathVariable String providerId,
        @ApiParam(value = ApiDocumentation.CommonParameters.CLOUD_NAME)
        @PathVariable String cloudName,
        @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
        @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws Exception{
        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_RESOURCE_S_REQUEST, QUOTA_SUFFIX_ENDPOINT, providerId));
            ResourceQuota resourceQuota = ApplicationFacade.getInstance().getResourceQuota(providerId, cloudName, systemUserToken);
            return new ResponseEntity<>(resourceQuota, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
    
}
