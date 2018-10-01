package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = PublicIp.PUBLIC_IP_ENDPOINT)
@Api(description = "Manage public ip resources")
public class PublicIp {

    public static final String PUBLIC_IP_ENDPOINT = "publicIps";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String ORDER_CONTROLLER_TYPE = "publicip";

    private final Logger LOGGER = Logger.getLogger(PublicIp.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createPublicIp(@RequestBody org.fogbowcloud.ras.api.parameters.PublicIp publicIp,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_CREATE_REQUEST, ORDER_CONTROLLER_TYPE));
        String publicIpId = ApplicationFacade.getInstance().createPublicIp(publicIp.getOrder(), federationTokenValue);
        return new ResponseEntity<String>(publicIpId, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.GET)
    public ResponseEntity<PublicIpInstance> getPublicIp(@PathVariable String publicIpId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_REQUEST, ORDER_CONTROLLER_TYPE, publicIpId));
        PublicIpInstance publicIpInstance =
                ApplicationFacade.getInstance().getPublicIp(publicIpId, federationTokenValue);
        return new ResponseEntity<>(publicIpInstance, HttpStatus.OK);
    }

    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deletePublicIp(@PathVariable String publicIpId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_DELETE_REQUEST, ORDER_CONTROLLER_TYPE, publicIpId));
        ApplicationFacade.getInstance().deletePublicIp(publicIpId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllPublicIpStatus(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, ORDER_CONTROLLER_TYPE));
        List<InstanceStatus> publicIpStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.PUBLIC_IP);
        return new ResponseEntity<>(publicIpStatus, HttpStatus.OK);
    }
}
