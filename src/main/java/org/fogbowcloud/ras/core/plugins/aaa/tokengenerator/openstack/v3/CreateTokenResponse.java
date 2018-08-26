package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Identity.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/identity/v3/
 * <p>
 * Response Example:
 * {
 * "token":{
 * "user":{
 * "id": "3324431f606d4a74a060cf78c16fcb2111",
 * "name": "user_name"
 * },
 * "project":{
 * "id": "3324431f606d4a74a060cf78c16fcb2111",
 * "name": "project_name"
 * }
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateTokenResponse {
    @SerializedName(TOKEN_KEY_JSON)
    private Token token;

    public static CreateTokenResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateTokenResponse.class);
    }

    public User getUser() {
        return this.token.user;
    }

    public Project getProject() {
        return this.token.project;
    }

    private class Token {
        @SerializedName(USER_KEY_JSON)
        private User user;
        @SerializedName(PROJECT_KEY_JSON)
        private Project project;
    }

    public class User {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public class Project {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
