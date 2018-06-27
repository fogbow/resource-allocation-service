package org.fogbowcloud.manager.core.models;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

public class ErrorResponseMap {

    private String responseString;
    private HttpResponse httpResponse;
    private Map<Integer, ErrorResponse> errorResponseMap;

    public ErrorResponseMap(HttpResponse httpResponse, String responseString) {
        this.responseString = responseString;
        this.httpResponse = httpResponse;
        this.errorResponseMap = new HashMap<>();

        fillStatusResponseMap();
    }

    private void fillStatusResponseMap() {
        errorResponseMap.put(
                HttpStatus.SC_UNAUTHORIZED,
                new ErrorResponse(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED));
        errorResponseMap.put(
                HttpStatus.SC_NOT_FOUND,
                new ErrorResponse(ErrorType.NOT_FOUND, ResponseConstants.NOT_FOUND));
        errorResponseMap.put(
                HttpStatus.SC_BAD_REQUEST,
                new ErrorResponse(ErrorType.BAD_REQUEST, responseString));

        if (responseString.contains(ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES)) {
            errorResponseMap.put(
                    HttpStatus.SC_REQUEST_TOO_LONG,
                    new ErrorResponse(
                            ErrorType.QUOTA_EXCEEDED,
                            ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES));
            errorResponseMap.put(
                    HttpStatus.SC_FORBIDDEN,
                    new ErrorResponse(
                            ErrorType.QUOTA_EXCEEDED,
                            ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES));
        } else {
            errorResponseMap.put(
                    HttpStatus.SC_REQUEST_TOO_LONG,
                    new ErrorResponse(ErrorType.BAD_REQUEST, responseString));
            errorResponseMap.put(
                    HttpStatus.SC_FORBIDDEN,
                    new ErrorResponse(ErrorType.BAD_REQUEST, responseString));
        }

        if (responseString.contains(ResponseConstants.NO_VALID_HOST_FOUND)) {
            errorResponseMap.put(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    new ErrorResponse(
                            ErrorType.NO_VALID_HOST_FOUND, ResponseConstants.NO_VALID_HOST_FOUND));
        }
    }

    public ErrorResponse getStatusResponse(Integer key) {
        if (key > 204) {
            return new ErrorResponse(
                    ErrorType.BAD_REQUEST,
                    "Status code: "
                            + httpResponse.getStatusLine().toString()
                            + " | Message:"
                            + httpResponse);
        } else {
            return errorResponseMap.get(key);
        }
    }
}
