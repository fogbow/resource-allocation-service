package org.fogbowcloud.ras.api.http;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.fogbowcloud.ras.api.http.GenericRequest.GENERIC_REQUEST_ENDPOINT;

@CrossOrigin
@RestController
@RequestMapping(value = GENERIC_REQUEST_ENDPOINT)
public class GenericRequest {

    public static final String GENERIC_REQUEST_ENDPOINT = "genericRequest";

    public static final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
    public static final String MEMBER_ID_HEADER_KEY = "memberId";

    private final Logger LOGGER = Logger.getLogger(GenericRequest.class);

    @RequestMapping("/**")
    public ResponseEntity<String> genericRequest(HttpServletRequest request,
                                                 @RequestHeader(required = true, value = MEMBER_ID_HEADER_KEY) String memberId,
                                                 @RequestBody(required = false) String body,
                                                 @RequestBody(required = false) String cloudName)
            throws UnexpectedException, FogbowRasException {
        Map<String, String> headers = HttpRequestClientUtil.getHeaders(request);

        String url = request.getRequestURL().toString();
        String method = request.getMethod();

        String response = ApplicationFacade.getInstance().genericRequest(cloudName, memberId, method, url, headers, body,
                null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
