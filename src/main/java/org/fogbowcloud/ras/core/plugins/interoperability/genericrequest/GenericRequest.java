package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import java.util.Map;

public class GenericRequest {
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> body;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getBody() {
        return body;
    }

    public void setBody(Map<String, String> body) {
        this.body = body;
    }
}