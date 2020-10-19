package cloud.fogbow.ras.api.http.request;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.ApplicationFacade;
import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
@RequestMapping(value = Admin.ADMIN_ENDPOINT)
public class Admin {
    public static final String ADMIN_SUFFIX_ENDPOINT = "admin";
    public static final String ADMIN_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + ADMIN_SUFFIX_ENDPOINT;
    
    private final Logger LOGGER = Logger.getLogger(Admin.class);
    
    @ApiOperation(value = ApiDocumentation.Admin.RELOAD_OPERATION)
    @RequestMapping(method = RequestMethod.POST)
    public void reload(String systemUserToken) {
        // TODO complete API documentation
        ApplicationFacade.getInstance().reload(systemUserToken);
    }
}
