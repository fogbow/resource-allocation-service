package cloud.fogbow.ras.api.http;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.InstanceStatus;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
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

    public static final String NETWORK_ENDPOINT = "networks";
    public static final String ORDER_CONTROLLER_TYPE = "network";

    public static final String SECURITY_RULES_ENDPOINT = "securityRules";
    public static final String SECURITY_RULE_NAME = "security rule";

    private final Logger LOGGER = Logger.getLogger(Network.class);

    @ApiOperation(value = ApiDocumentation.Network.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createNetwork(
            @ApiParam(value = ApiDocumentation.Network.CREATE_REQUEST_BODY)
            @RequestBody cloud.fogbow.ras.api.parameters.Network network,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String networkId = ApplicationFacade.getInstance().createNetwork(network.getOrder(), federationTokenValue);
            return new ResponseEntity<String>(networkId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.GET_OPERATION)
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllNetworksStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws UnavailableProviderException, UnexpectedException, InvalidTokenException,
            UnauthenticatedUserException, UnauthorizedRequestException, ConfigurationErrorException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> networkInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.NETWORK);
            return new ResponseEntity<>(networkInstanceStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{networkId}", method = RequestMethod.GET)
    public ResponseEntity<NetworkInstance> getNetwork(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, networkId));
            NetworkInstance networkInstance = ApplicationFacade.getInstance().getNetwork(networkId, federationTokenValue);
            return new ResponseEntity<>(networkInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.DELETE_OPERATION)
    @RequestMapping(value = "/{networkId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteNetwork(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, networkId));
            ApplicationFacade.getInstance().deleteNetwork(networkId, federationTokenValue);
            return new ResponseEntity<Boolean>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.CREATE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{networkId}/" + SECURITY_RULES_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<String> createSecurityRule(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.Network.CREATE_SECURITY_RULE_REQUEST_BODY)
            @RequestBody SecurityRule securityRule,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, SECURITY_RULE_NAME));
            String ruleId = ApplicationFacade.getInstance().createSecurityRule(networkId, securityRule,
                    federationTokenValue, ResourceType.NETWORK);
            return new ResponseEntity<String>(ruleId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.GET_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{networkId}/" + SECURITY_RULES_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<SecurityRule>> getAllSecurityRules(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, SECURITY_RULE_NAME));
            List<SecurityRule> securityRules = ApplicationFacade.getInstance().
                    getAllSecurityRules(networkId, federationTokenValue, ResourceType.NETWORK);
            return new ResponseEntity<>(securityRules, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.Network.DELETE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{networkId}/" + SECURITY_RULES_ENDPOINT + "/{ruleId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteSecurityRule(
            @ApiParam(value = ApiDocumentation.Network.ID)
            @PathVariable String networkId,
            @ApiParam(value = ApiDocumentation.Network.SECURITY_RULE_ID)
            @PathVariable String ruleId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = CommonKeys.FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, SECURITY_RULE_NAME, ruleId));
            ApplicationFacade.getInstance().deleteSecurityRule(networkId, ruleId, federationTokenValue,
                    ResourceType.NETWORK);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()), e);
            throw e;
        }
    }
}
