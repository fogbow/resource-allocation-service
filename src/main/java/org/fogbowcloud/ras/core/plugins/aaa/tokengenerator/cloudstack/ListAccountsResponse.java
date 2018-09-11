package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Identity.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listAccounts.html
 * {
 *   "account": [
 *     {
 *       "name": "jondoe@lsd.ufcg.edu.br",
 *       "user": [
 *         {
 *           "account": "jondoe@myemail.com",
 *           "accountid": "accountid",
 *           "accounttype": 2,
 *           "apikey": "apikey",
 *           "created": "2016-10-17T12:28:48-0200",
 *           "domain": "FOGBOW",
 *           "domainid": "domainid",
 *           "email": "jondoe@myemail.com",
 *           "firstname": "Jon",
 *           "id": "userid",
 *           "iscallerchilddomain": false,
 *           "isdefault": false,
 *           "lastname": "Doe",
 *           "roleid": "roleid",
 *           "rolename": "Domain Admin",
 *           "roletype": "DomainAdmin",
 *           "secretkey": "secretkey",
 *           "state": "enabled",
 *           "username": "jondoe@myemail.com"
 *         }
 *       ],
 *     }
 *   ],
 *   "count": 1
 * }
 */
public class ListAccountsResponse {
    @SerializedName(ACCOUNT_KEY_JSON)
    private List<Account> listAccountsResponse;

    public class Account {
        @SerializedName(USER_KEY_JSON)
        private List<User> users;

        public List<User> getUsers() {
            return users;
        }
    }

    public List<Account> getAccounts() {
        return this.listAccountsResponse;
    }

    public class User {
        @SerializedName(USER_ID_KEY_JSON)
        private String id;
        @SerializedName(USERNAME_KEY_JSON)
        private String username;
        @SerializedName(FIRST_NAME_KEY_JSON)
        private String firstName;
        @SerializedName(LAST_NAME_KEY_JSON)
        private String lastName;
        @SerializedName(API_KEY_JSON)
        private String apiKey;
        @SerializedName(SECRET_KEY_JSON)
        private String secretKey;

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getSecretKey() {
            return secretKey;
        }
    }

    public static ListAccountsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, ListAccountsResponse.class);
    }
}
