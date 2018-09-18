package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Identity.*;

/**
 * 
 * Documentation : https://developer.openstack.org/api-ref/identity/v3/#password-authentication-with-scoped-authorization
 * 
 * Request Example:   
 * {
 *     "auth":{
 *         "identity":{
 *             "methods":[
 *                 "password"
 *             ],
 *             "password":{
 *                 "user":{
 *                     "domain":{
 *                         "name":"name"
 *                     },
 *                     "name":"username",
 *                     "password":"password"
 *                 }
 *             }
 *         },
 *         "scope":{
 *             "project":{
 *                 "domain":{
 *                     "name":"domain"
 *                 },
 *                 "name":"project-name"
 *             }
 *         }
 *     }
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
        Identity identity = new Identity(methods, password);

        Project project = new Project(domain, builder.projectName);
        Scope scope = new Scope(project);

        this.auth = new Auth(identity, scope);
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CreateTokenRequest request = (CreateTokenRequest) o;

        return auth != null ? auth.equals(request.auth) : request.auth == null;
    }

    private static class Auth {
        @SerializedName(SCOPE_KEY_JSON)
        private Scope scope;
        @SerializedName(IDENTITY_KEY_JSON)
        private Identity identity;

        public Auth(Identity identity, Scope scope) {
            this.identity = identity;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Auth auth = (Auth) o;

            if (scope != null ? !scope.equals(auth.scope) : auth.scope != null) {
                return false;
            }
            return identity != null ? identity.equals(auth.identity) : auth.identity == null;
        }
    }

    private static class Scope {
        @SerializedName(PROJECT_KEY_JSON)
        private Project project;

        public Scope(Project project) {
            this.project = project;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Scope scope = (Scope) o;

            return project != null ? project.equals(scope.project) : scope.project == null;
        }
    }

    private static class Identity {
        @SerializedName(PASSWORD_KEY_JSON)
        private Password password;
        @SerializedName(METHODS_KEY_JSON)
        private String[] methods;

        public Identity(String[] methods, Password password) {
            this.methods = methods;
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Identity identity = (Identity) o;

            if (password != null ? !password.equals(identity.password)
                : identity.password != null) {
                return false;
            }
            return Arrays.equals(methods, identity.methods);
        }

    }

    private static class Project {
        @SerializedName(DOMAIN_KEY_JSON)
        private Domain domain;
        @SerializedName(NAME_KEY_JSON)
        private String name;

        public Project(Domain domain, String name) {
            this.domain = domain;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Project project = (Project) o;

            if (domain != null ? !domain.equals(project.domain) : project.domain != null) {
                return false;
            }
            return name != null ? name.equals(project.name) : project.name == null;
        }
    }

    private static class Password {
        @SerializedName(USER_KEY_JSON)
        private User user;

        public Password(User user) {
            this.user = user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Password password = (Password) o;

            return user != null ? user.equals(password.user) : password.user == null;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            User user = (User) o;

            if (name != null ? !name.equals(user.name) : user.name != null) {
                return false;
            }
            if (password != null ? !password.equals(user.password) : user.password != null) {
                return false;
            }
            return domain != null ? domain.equals(user.domain) : user.domain == null;
        }
    }

    public static class Domain {
        @SerializedName(NAME_KEY_JSON)
        private String name;

        public Domain(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Domain domain = (Domain) o;

            return name != null ? name.equals(domain.name) : domain.name == null;
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
