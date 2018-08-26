package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = VolumeOrdersController.VOLUME_ENDPOINT)
public class VolumeOrdersController {

    public static final String VOLUME_ENDPOINT = "volumes";
    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(VolumeOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createVolume(@RequestBody VolumeOrder volumeOrder,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info("New volume order request received <" + volumeOrder.getId() + ">.");
        String volumeId = ApplicationFacade.getInstance().createVolume(volumeOrder, federationTokenValue);
        return new ResponseEntity<String>(volumeId, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<List<InstanceStatus>> getAllVolumesStatus(
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get the status of all volume order requests received.");
        List<InstanceStatus> volumeInstanceStatus =
                ApplicationFacade.getInstance().getAllInstancesStatus(federationTokenValue, ResourceType.VOLUME);
        return new ResponseEntity<>(volumeInstanceStatus, HttpStatus.OK);
    }

    @RequestMapping(value = "/{volumeId}", method = RequestMethod.GET)
    public ResponseEntity<VolumeInstance> getVolume(@PathVariable String volumeId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get request for volume order <" + volumeId + "> received.");
        VolumeInstance volume = ApplicationFacade.getInstance().getVolume(volumeId, federationTokenValue);
        return new ResponseEntity<>(volume, HttpStatus.OK);
    }

    @RequestMapping(value = "/{volumeId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteVolume(@PathVariable String volumeId,
            @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info("Delete compute order <" + volumeId + "> received.");
        ApplicationFacade.getInstance().deleteVolume(volumeId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
