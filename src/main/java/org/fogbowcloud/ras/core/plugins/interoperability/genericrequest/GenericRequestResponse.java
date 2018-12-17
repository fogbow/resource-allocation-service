package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

public class GenericRequestResponse {
    private String content;

    public GenericRequestResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
