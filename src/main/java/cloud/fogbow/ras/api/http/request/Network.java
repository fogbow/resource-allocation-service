package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.response.ResourceId;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
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
@RequestMapping(value = Network.NETWORK_ENDPOINT)
@Api(description = ApiDocumentation.Network.API)
public class Network {
    public static final String NETWORK_SUFFIX_ENDPOINT = "networks";
    public static final String NETWORK_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + NETWORK_SUFFIX_ENDPOINT;
    public static final String ORDER_CONTROLLER_TYPE = "network";

    public static final String SECURITY_RULES_SUFFIX_ENDPOINT = "securityRules";
    public static final String SECURITY_RULE_NAME = "security rule";

    private final Logger LOGGER = Logger.getLogger(Network.class);

    // HttpExceptionToErrorConditionTranslator handles the possible problems in request

    @ApiOperation(value = ApiDocumentation.Network.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createNetwork(
            @ApiParam(value = ApiDocumentation.Network.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Network network,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String networkId = ApplicationFacade.getInstance().createNetwork(network.getOrder(), systemUserToken);
            return new ResponseEntity<>(new ResourceId(networkId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.GET_OPERATION)
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllNetworksStatus(
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            List<InstanceStatus> networkInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(systemUserToken, ResourceType.NETWORK);
            return new ResponseEntity<>(networkInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{networkId}", method = RequestMethod.GET)
    public ResponseEntity<NetworkInstance> getNetwork(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, networkId));
            NetworkInstance networkInstance = ApplicationFacade.getInstance().getNetwork(networkId, systemUserToken);
            return new ResponseEntity<>(networkInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.DELETE_OPERATION)
    @RequestMapping(value = "/{networkId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteNetwork(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, networkId));
            ApplicationFacade.getInstance().deleteNetwork(networkId, systemUserToken);
            return new ResponseEntity<Boolean>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.CREATE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{networkId}/" + SECURITY_RULES_SUFFIX_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<ResourceId> createSecurityRule(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.Network.CREATE_SECURITY_RULE_REQUEST_BODY)
            @RequestBody SecurityRule securityRule,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, SECURITY_RULE_NAME));
            String ruleId = ApplicationFacade.getInstance().createSecurityRule(networkId, securityRule,
                    systemUserToken, ResourceType.NETWORK);
            return new ResponseEntity<>(new ResourceId(ruleId), HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.GET_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{networkId}/" + SECURITY_RULES_SUFFIX_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<SecurityRuleInstance>> getAllSecurityRules(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, SECURITY_RULE_NAME));
            List<SecurityRuleInstance> securityRuleInstances = ApplicationFacade.getInstance().
                    getAllSecurityRules(networkId, systemUserToken, ResourceType.NETWORK);
            return new ResponseEntity<>(securityRuleInstances, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.DELETE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{networkId}/" + SECURITY_RULES_SUFFIX_ENDPOINT + "/{ruleId:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteSecurityRule(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.Network.SECURITY_RULE_ID)
            @PathVariable String ruleId,
            @ApiParam(value = cloud.fogbow.common.constants.ApiDocumentation.Token.SYSTEM_USER_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, SECURITY_RULE_NAME, ruleId));
            ApplicationFacade.getInstance().deleteSecurityRule(networkId, ruleId, systemUserToken,
                    ResourceType.NETWORK);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
