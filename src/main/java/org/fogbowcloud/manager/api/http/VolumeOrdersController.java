package org.fogbowcloud.manager.api.http;

import java.util.List;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = VolumeOrdersController.VOLUME_ENDPOINT)
public class VolumeOrdersController {

    public static final String VOLUME_ENDPOINT = "volumes";

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";

    private final Logger LOGGER = Logger.getLogger(VolumeOrdersController.class);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createVolume(@RequestBody VolumeOrder volumeOrder,
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("New volume order request received <" + volumeOrder.getId() + ">.");

        String volumeId = ApplicationFacade.getInstance().createVolume(volumeOrder, federationTokenValue);
        return new ResponseEntity<String>(volumeId, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<VolumeInstance>> getAllVolumes(
        @RequestHeader(required = false, value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
            throws Exception {
        LOGGER.info("Get all volume order requests received.");
        List<VolumeInstance> volumes = ApplicationFacade.getInstance().getAllVolumes(federationTokenValue);
        return new ResponseEntity<>(volumes, HttpStatus.OK);
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
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("Delete compute order <" + volumeId + "> received.");
        ApplicationFacade.getInstance().deleteVolume(volumeId, federationTokenValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
