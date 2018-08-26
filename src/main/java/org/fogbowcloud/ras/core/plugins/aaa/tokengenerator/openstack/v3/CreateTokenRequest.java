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
    @SerializedName(AUTH_KEY_JSON)
    private Auth auth;

    public CreateTokenRequest(Auth auth) {
        this.auth = auth;
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

        public Auth(Builder builder) {
            this.scope = new Scope(builder);
            this.identity = new Identity(builder);
        }
    }

    private static class Scope {
        @SerializedName(PROJECT_KEY_JSON)
        private Project project;

        public Scope(Builder builder) {
            this.project = new Project(builder);
        }
    }

    private static class Identity {
        @SerializedName(PASSWORD_KEY_JSON)
        private Password password;
        @SerializedName(METHODS_KEY_JSON)
        private String[] methods;

        public Identity(Builder builder) {
            this.password = new Password(builder);
            this.methods = new String[]{METHODS_PASSWORD_VALUE_JSON};
        }
    }

    private static class Project {
        @SerializedName(ID_KEY_JSON)
        private String id;

        public Project(Builder builder) {
            this.id = builder.projectId;
        }
    }

    private static class Password {
        @SerializedName(USER_KEY_JSON)
        private User user;

        public Password(Builder builder) {
            this.user = new User(builder);
        }
    }

    private static class User {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(PASSWORD_KEY_JSON)
        private String password;

        public User(Builder builder) {
            this.id = builder.userId;
            this.password = builder.password;
        }
    }

    public static class Builder {
        private String projectId;
        private String userId;
        private String password;

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public CreateTokenRequest build() {
            return new CreateTokenRequest(new Auth(this));
        }
    }
}
