package org.fogbowcloud.manager.core.models.tokens;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Token {

    private static final String EXPIRATION_DATE = "expirationDate";

    private Long id;

    private Map<String, String> attributes;

    private String accessId;

    private User user;

    public Token() {}

    public Token(String accessId, User user, Date expirationTime, Map<String, String> attributes) {
        this.accessId = accessId;
        this.user = user;
        if (attributes == null) {
            this.attributes = new HashMap<>();
        } else {
            this.attributes = attributes;
        }

        this.attributes.put(Token.EXPIRATION_DATE, String.valueOf(expirationTime.getTime()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String get(String attributeName) {
        return attributes.get(attributeName);
    }

    public String getAccessId() {
        return this.accessId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Token [attributes=" + attributes + ", " + "user=" + user + "]";
    }

    public User getUser() {
        return this.user;
    }

    public static class User {

        private String id;

        private String name;

        public User(String id, String name) {
            if (id == null || name == null) {
                throw new IllegalArgumentException(
                        "User id (" + id + ") and " + "name (" + name + ") are not null.");
            }
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            User otherUser = (User) obj;
            if (!id.equals(otherUser.getId()) || !name.equals(otherUser.getName())) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "User [id=" + id + ", name=" + name + "]";
        }
    }
}
