package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Identity.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/identity/v3/
 * <p>
 * Request Example:
 * {
 * "auth":{
 * "scope":{
 * "project":{
 * "id":"3324431f606d4a74a060cf78c16fcb2111"
 * }
 * },
 * "identity":{
 * "methods":[
 * "password"
 * ],
 * "password":{
 * "user":{
 * "id":"6642431f606d4a74a060cf78c16fcb2121",
 * "password":"password"
 * }
 * }
 * }
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateTokenRequest implements JsonSerializable {

    public static final String PASSWORD_AUTHENTICATION_METHOD = "password";
    @SerializedName(AUTH_KEY_JSON)
    private Auth auth;

    public CreateTokenRequest(Builder builder) {
        Domain domain = new Domain(builder.domain);
        User user = new User(builder.userName, builder.password, domain);
        Password password = new Password(user);

        String[] methods = new String[] { PASSWORD_AUTHENTICATION_METHOD };
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class Auth {
        @SerializedName(SCOPE_KEY_JSON)
        private Scope scope;
        @SerializedName(IDENTITY_KEY_JSON)
        private Identity identity;
    }

    private static class Scope {
        @SerializedName(PROJECT_KEY_JSON)
        private Project project;
    }

    private static class Identity {
        @SerializedName(PASSWORD_KEY_JSON)
        private Password password;
        @SerializedName(METHODS_KEY_JSON)
        private String[] methods;
    }

    private static class Project {
        @SerializedName(DOMAIN_KEY_JSON)
        private Domain domain;
        @SerializedName(NAME_KEY_JSON)
        private String name;
    }

    private static class Password {
        @SerializedName(USER_KEY_JSON)
        private User user;

        public Password(User user) {
            this.user = user;
        }
    }

    private static class User {
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(PASSWORD_KEY_JSON)
        private String password;
        @SerializedName(DOMAIN_KEY_JSON)
        private Domain domain;

        public User(String name, String password, Domain domain) {
            this.name = name;
            this.password = password;
            this.domain = domain;
        }
    }

    public static class Domain {
        @SerializedName(NAME_KEY_JSON)
        private String name;

        public Domain(String name) {
            this.name = name;
        }
    }

    public static class Builder {
        private String projectName;
        private String userName;
        private String password;
        private String domain;

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public CreateTokenRequest build() {
            return new CreateTokenRequest(this);
        }
    }
}
