package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;


public class LoginRequest extends CloudStackRequest {
    public static final String LOGIN_COMMAND = "login";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String DOMAIN_KEY = "domain";

    private LoginRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(USERNAME_KEY, builder.username);
        addParameter(PASSWORD_KEY, builder.password);
        addParameter(DOMAIN_KEY, builder.domain);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return LOGIN_COMMAND;
    }

    public static class Builder {

        private String username;
        private String password;
        private String domain;

        public Builder username(String username) {
            this.username = username;
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

        public LoginRequest build() throws InvalidParameterException {
            return new LoginRequest(this);
        }

    }
}
