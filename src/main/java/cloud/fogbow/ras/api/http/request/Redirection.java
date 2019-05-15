package cloud.fogbow.ras.api.http.request;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.util.RedirectSwaggerDocumentationUtil;
import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

@CrossOrigin(methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS,
        RequestMethod.PUT})
@Controller
@ApiIgnore
public class Redirection {
    private static final Logger LOGGER = Logger.getLogger(Redirection.class);

    @RequestMapping(value = SystemConstants.SERVICE_BASE_ENDPOINT + "doc")
    public ResponseEntity redirectDocumentationRequest(@RequestBody(required = false) String body, HttpMethod method,
                                                       HttpServletRequest request) throws URISyntaxException, FatalErrorException,
            FogbowException {
        try {
            LOGGER.info(Messages.Info.REDIRECT_SWAGGER_DOCUMENTATION);

            return RedirectSwaggerDocumentationUtil.redirectRequest(body, method, request, String.class);
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            throw e;
        }
    }
}
