package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ResourceId;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.ResourceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = PublicIp.PUBLIC_IP_ENDPOINT)
@Api(description = ApiDocumentation.PublicIp.API)
public class PublicIp {
    public static final String PUBLIC_IP_SUFFIX_ENDPOINT = "publicIps";
    public static final String PUBLIC_IP_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + PUBLIC_IP_SUFFIX_ENDPOINT;
    public static final String ORDER_CONTROLLER_TYPE = "publicip";

    public static final String SECURITY_RULES_SUFFIX_ENDPOINT = "securityRules";
    public static final String SECURITY_RULE_NAME = "security rule";

    private final Logger LOGGER = Logger.getLogger(PublicIp.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @ApiOperation(value = ApiDocumentation.PublicIp.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createPublicIp(
            @ApiParam(value = ApiDocumentation.PublicIp.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.PublicIp publicIp,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String publicIpId = ApplicationFacade.getInstance().createPublicIp(publicIp.getOrder(), systemUserToken);
            return new ResponseEntity<>(new ResourceId(publicIpId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.GET)
    public ResponseEntity<PublicIpInstance> getPublicIp(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, publicIpId));
            PublicIpInstance publicIpInstance =
                ApplicationFacade.getInstance().getPublicIp(publicIpId, systemUserToken);
            return new ResponseEntity<>(publicIpInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.DELETE_OPERATION)
    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deletePublicIp(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, publicIpId));
            ApplicationFacade.getInstance().deletePublicIp(publicIpId, systemUserToken);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.GET_OPERATION)
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllPublicIpStatus(
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> publicIpStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(systemUserToken, ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(publicIpStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.CREATE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{publicIpId}/" + SECURITY_RULES_SUFFIX_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createSecurityRule(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.PublicIp.CREATE_SECURITY_RULE_REQUEST_BODY)
            @RequestBody SecurityRule securityRule,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, SECURITY_RULE_NAME));
            String ruleId = ApplicationFacade.getInstance().createSecurityRule(publicIpId, securityRule,
                    systemUserToken, ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(new ResourceId(ruleId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.GET_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{publicIpId}/" + SECURITY_RULES_SUFFIX_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<SecurityRuleInstance>> getAllSecurityRules(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, SECURITY_RULE_NAME));
            List<SecurityRuleInstance> securityRuleInstances = ApplicationFacade.getInstance().
                    getAllSecurityRules(publicIpId, systemUserToken, ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(securityRuleInstances, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.DELETE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{publicIpId}/" + SECURITY_RULES_SUFFIX_ENDPOINT + "/{ruleId:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteSecurityRule(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.PublicIp.SECURITY_RULE_ID)
            @PathVariable String ruleId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, SECURITY_RULE_NAME, ruleId));
            ApplicationFacade.getInstance().deleteSecurityRule(publicIpId, ruleId, systemUserToken,
                    ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
