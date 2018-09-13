package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = PublicIpOrdersController.PUBLIC_IP_ENDPOINT)
public class PublicIpOrdersController {

    public static final String PUBLIC_IP_ENDPOINT = "publicIps";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(PublicIpOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createPublicIp(@RequestBody PublicIpOrder publicIpOrder,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("New publicIp order request received.");
        String publicIpId = ApplicationFacade.getInstance().createPublicIp(publicIpOrder, federationTokenValue);
        return new ResponseEntity<String>(publicIpId, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.GET)
    public ResponseEntity<PublicIpInstance> getPublicIp(@PathVariable String publicIpId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get request for publicIp order <" + publicIpId + "> received.");
        PublicIpInstance publicIpInstance =
                ApplicationFacade.getInstance().getPublicIp(publicIpId, federationTokenValue);
        return new ResponseEntity<>(publicIpInstance, HttpStatus.OK);
    }

    @RequestMapping(value = "/{publicIpId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deletePublicIp(@PathVariable String publicIpId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Delete publicIp order <" + publicIpId + "> received.");
        ApplicationFacade.getInstance().deletePublicIp(publicIpId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllPublicIpStatus(
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
        throws Exception {
        LOGGER.info("Get the status of all attachment order requests received.");
        List<InstanceStatus> publicIpStatus =
            ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.PUBLIC_IP);
        return new ResponseEntity<>(publicIpStatus, HttpStatus.OK);
    }
}
