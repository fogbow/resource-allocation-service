package org.fogbowcloud.manager.core.models.orders;

import org.apache.commons.codec.binary.Base64;

public class UserData {

    private Long id;
    private String content;

    public UserData(String content) {
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return Base64.encodeBase64String(content.getBytes());
    }

    public void setContent(String content) {
        this.content = content;
    }
}
