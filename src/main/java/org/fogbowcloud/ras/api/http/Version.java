package org.fogbowcloud.ras.api.http;

import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.constants.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value = Version.VERSION_ENDPOINT)
@Api(description = "Query RAS version")
public class Version {

    public static final String VERSION_ENDPOINT = "version";

    private final Logger LOGGER = Logger.getLogger(Version.class);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> getVersion() {
        LOGGER.info(Messages.Info.RECEIVING_GET_VERSION_REQUEST);
        String versionNumber = ApplicationFacade.getInstance().getVersionNumber();
        return new ResponseEntity<>(versionNumber, HttpStatus.OK);
    }
}
