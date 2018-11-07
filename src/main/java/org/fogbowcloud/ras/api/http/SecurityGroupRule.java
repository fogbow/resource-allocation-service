package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = SecurityGroupRule.SECURITY_GROUP_ENDPOINT)
public class SecurityGroupRule {

    Logger LOGGER = Logger.getLogger(SecurityGroupRule.class);

    public static final String SECURITY_GROUP_ENDPOINT = "securityGroups";
    public static final String SECURITY_GROUP_RULE_ENDPOINT = "securityGroupRules";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "security group rule";

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULE_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<SecurityGroupRule> getAllSecurityGroupRules (
            @PathVariable String orderId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue) {

        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));

        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULE_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<SecurityGroupRule> createSecurityGroupRule (
            @RequestBody SecurityGroupRule securityGroupRule,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue) {

        LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));

        throw new UnsupportedOperationException();
    }

    @RequestMapping(value = "/{orderId}/" + SECURITY_GROUP_RULE_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<SecurityGroupRule> deleteSecurityGroupRule (
            @PathVariable String orderId,
            @PathVariable String securityGroupId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue) {

        LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, securityGroupId));

        throw new UnsupportedOperationException();
    }

}
