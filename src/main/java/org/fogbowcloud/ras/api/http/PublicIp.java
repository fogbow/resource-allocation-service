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
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = PublicIp.PUBLIC_IP_ENDPOINT)
@Api(description = ApiDocumentation.PublicIp.API)
public class PublicIp {

    public static final String PUBLIC_IP_ENDPOINT = "publicIps";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "publicip";

    public static final String SECURITY_RULES_ENDPOINT = "securityRules";
    public static final String SECURITY_RULE_NAME = "security rule";

    private final Logger LOGGER = Logger.getLogger(PublicIp.class);

    @ApiOperation(value = ApiDocumentation.PublicIp.CREATE_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createPublicIp(
            @ApiParam(value = ApiDocumentation.PublicIp.CREATE_REQUEST_BODY)
            @RequestBody org.fogbowcloud.ras.api.parameters.PublicIp publicIp,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
            String publicIpId = ApplicationFacade.getInstance().createPublicIp(publicIp.getOrder(), federationTokenValue);
            return new ResponseEntity<String>(publicIpId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.GET_BY_ID_OPERATION)
    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.GET)
    public ResponseEntity<PublicIpInstance> getPublicIp(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, publicIpId));
            PublicIpInstance publicIpInstance =
                ApplicationFacade.getInstance().getPublicIp(publicIpId, federationTokenValue);
            return new ResponseEntity<>(publicIpInstance, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.DELETE_OPERATION)
    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deletePublicIp(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, publicIpId));
            ApplicationFacade.getInstance().deletePublicIp(publicIpId, federationTokenValue);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.GET_OPERATION)
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllPublicIpStatus(
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
            List<InstanceStatus> publicIpStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(publicIpStatus, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.CREATE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{publicIpId}/" + SECURITY_RULES_ENDPOINT, method = RequestMethod.POST)
    public ResponseEntity<String> createSecurityRule(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.PublicIp.CREATE_SECURITY_RULE_REQUEST_BODY)
            @RequestBody SecurityRule securityRule,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, SECURITY_RULE_NAME));
            String ruleId = ApplicationFacade.getInstance().createSecurityRule(publicIpId, securityRule,
                    federationTokenValue, ResourceType.PUBLIC_IP);
            return new ResponseEntity<String>(ruleId, HttpStatus.CREATED);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.GET_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{publicIpId}/" + SECURITY_RULES_ENDPOINT, method = RequestMethod.GET)
    public ResponseEntity<List<SecurityRule>> getAllSecurityRules(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, SECURITY_RULE_NAME));
            List<SecurityRule> securityRules = ApplicationFacade.getInstance().
                    getAllSecurityRules(publicIpId, federationTokenValue, ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(securityRules, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }

    @ApiOperation(value = ApiDocumentation.PublicIp.DELETE_SECURITY_RULE_OPERATION)
    @RequestMapping(value = "/{publicIpId}/" + SECURITY_RULES_ENDPOINT + "/{ruleId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteSecurityRule(
            @ApiParam(value = ApiDocumentation.PublicIp.ID)
            @PathVariable String publicIpId,
            @ApiParam(value = ApiDocumentation.PublicIp.SECURITY_RULE_ID)
            @PathVariable String ruleId,
            @ApiParam(value = ApiDocumentation.CommonParameters.FEDERATION_TOKEN)
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {

        try {
            LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, SECURITY_RULE_NAME, ruleId));
            ApplicationFacade.getInstance().deleteSecurityRule(publicIpId, ruleId, federationTokenValue,
                    ResourceType.PUBLIC_IP);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }
}
