package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = "{resource:" + SecurityGroupRule.NETWORK_OR_PUBLIC_IP_REGEX + "}")
public class SecurityGroupRule {

    Logger LOGGER = Logger.getLogger(SecurityGroupRule.class);

    public static final String SECURITY_GROUP_RULES_ENDPOINT = "securityGroupRules";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "security group rule";
    public static final String NETWORK_OR_PUBLIC_IP_REGEX = Network.NETWORK_ENDPOINT + "|" +
            PublicIp.PUBLIC_IP_ENDPOINT;

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULES_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<SecurityGroupRule> createSecurityGroupRule (
            @PathVariable String orderId,
            @RequestBody SecurityGroupRule securityGroupRule,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue) {

        LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));

        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULES_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<SecurityGroupRule> getAllSecurityGroupRules (
            @PathVariable("orderId") String orderId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue) {

        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));

        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULES_ENDPOINT + "/{ruleId}", method = RequestMethod.POST)
    public ResponseEntity<SecurityGroupRule> deleteSecurityGroupRule (
            @PathVariable String ruleId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue) {

        LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, ruleId));

        throw new UnsupportedOperationException();
    }

}
