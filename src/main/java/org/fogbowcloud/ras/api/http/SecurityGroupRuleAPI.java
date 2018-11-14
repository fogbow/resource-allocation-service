package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = "{resource:" + SecurityGroupRuleAPI.NETWORK_OR_PUBLIC_IP_REGEX + "}")
public class SecurityGroupRuleAPI {

    Logger LOGGER = Logger.getLogger(SecurityGroupRuleAPI.class);

    public static final String SECURITY_GROUP_RULES_ENDPOINT = "securityGroupRules";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "security group rule";
    public static final String NETWORK_OR_PUBLIC_IP_REGEX = Network.NETWORK_ENDPOINT + "|" +
            PublicIp.PUBLIC_IP_ENDPOINT;

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULES_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<String> createSecurityGroupRule (
            @PathVariable String orderId,
            @RequestBody SecurityGroupRule securityGroupRule,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String ruleId = ApplicationFacade.getInstance().createSecurityGroupRules(orderId, securityGroupRule,
                    federationTokenValue);
            return new ResponseEntity<String>(ruleId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }

    }

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULES_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<SecurityGroupRule>> getAllSecurityGroupRules (
            @PathVariable String orderId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<SecurityGroupRule> securityGroupRules = ApplicationFacade.getInstance().
                    getAllSecurityGroupRules(orderId, federationTokenValue);
            return new ResponseEntity<>(securityGroupRules, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULES_ENDPOINT + "/{ruleId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteSecurityGroupRule (
            @PathVariable String orderId,
            @PathVariable String ruleId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, ruleId));
            ApplicationFacade.getInstance().deleteSecurityGroupRules(orderId, ruleId, federationTokenValue);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

}
