package org.fogbowcloud.manager.core.models;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

public class StatusResponseMap {

    private String responseString;
    private HttpResponse httpResponse;
    private Map<Integer, StatusResponse> statusResponseMap;

    public StatusResponseMap(HttpResponse httpResponse, String responseString) {
        this.responseString = responseString;
        this.httpResponse = httpResponse;
        this.statusResponseMap = new HashMap<>();

        fillStatusResponseMap();
    }

    private void fillStatusResponseMap() {
        statusResponseMap.put(
                HttpStatus.SC_UNAUTHORIZED,
                new StatusResponse(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED));
        statusResponseMap.put(
                HttpStatus.SC_NOT_FOUND,
                new StatusResponse(ErrorType.NOT_FOUND, ResponseConstants.NOT_FOUND));
        statusResponseMap.put(
                HttpStatus.SC_BAD_REQUEST,
                new StatusResponse(ErrorType.BAD_REQUEST, responseString));

        if (responseString.contains(ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES)) {
            statusResponseMap.put(
                    HttpStatus.SC_REQUEST_TOO_LONG,
                    new StatusResponse(
                            ErrorType.QUOTA_EXCEEDED,
                            ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES));
            statusResponseMap.put(
                    HttpStatus.SC_FORBIDDEN,
                    new StatusResponse(
                            ErrorType.QUOTA_EXCEEDED,
                            ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES));
        } else {
            statusResponseMap.put(
                    HttpStatus.SC_REQUEST_TOO_LONG,
                    new StatusResponse(ErrorType.BAD_REQUEST, responseString));
            statusResponseMap.put(
                    HttpStatus.SC_FORBIDDEN,
                    new StatusResponse(ErrorType.BAD_REQUEST, responseString));
        }

        if (responseString.contains(ResponseConstants.NO_VALID_HOST_FOUND)) {
            statusResponseMap.put(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    new StatusResponse(
                            ErrorType.NO_VALID_HOST_FOUND, ResponseConstants.NO_VALID_HOST_FOUND));
        }
    }

    public StatusResponse getStatusResponse(Integer key) {
        if (key > 204) {
            return new StatusResponse(
                    ErrorType.BAD_REQUEST,
                    "Status code: "
                            + httpResponse.getStatusLine().toString()
                            + " | Message:"
                            + httpResponse);
        } else {
            return statusResponseMap.get(key);
        }
    }
}
