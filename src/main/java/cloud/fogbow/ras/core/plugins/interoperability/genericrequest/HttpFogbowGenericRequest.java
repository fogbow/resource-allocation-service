package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.ras.constants.Messages;

import java.util.HashMap;

public class HttpFogbowGenericRequest implements FogbowGenericRequest, Cloneable {

    private HttpMethod method;
    private String url;
    private HashMap<String, String> headers;
    private HashMap<String, String> body;

    public HttpFogbowGenericRequest(HttpMethod method, String url, HashMap<String, String> body, HashMap<String, String> headers) {
        if (headers == null || body == null) {
            throw new IllegalArgumentException("Neither body or headers can be null");
        }

        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }


    public HttpFogbowGenericRequest(HttpMethod method, String url, HashMap<String, String> body) {
        this(method, url, body, new HashMap<>());
    }

    public HttpFogbowGenericRequest(HttpMethod method, String url) {
        this(method, url, new HashMap<>(), new HashMap<>());
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public HashMap<String, String> getBody() {
        return body;
    }

    public void setBody(HashMap<String, String> body) {
        this.body = body;
    }

    @Override
    public Object clone() {
        try {
            HttpFogbowGenericRequest cloned = (HttpFogbowGenericRequest) super.clone();
            cloned.headers = (HashMap<String, String>) this.headers.clone();
            cloned.body = (HashMap<String, String>) this.body.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(String.format(Messages.Exception.CLASS_S_SHOULD_BE_CLONEABLE, this.getClass().getName()));
        }
    }
}
