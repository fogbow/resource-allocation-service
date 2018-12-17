package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

public class GenericRequestHttpResponse extends GenericRequestResponse {
    private int httpCode;

    public GenericRequestHttpResponse(String actualResponse, int httpCode) {
        super(actualResponse);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }
}
