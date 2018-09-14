package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Identity.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listAccounts.html
 * {
 * "listaccountsresponse": {
 * "count": 1,
 * "account": [{
 * "user": [{
 * "id": "anid",
 * "username": "ausername@usernames.com",
 * "firstname": "Jon",
 * "lastname": "Doe",
 * "email": "anemail@emails.com",
 * "created": "2016-10-17T12:28:48-0200",
 * "state": "enabled",
 * "account": "account@accounts.com",
 * "accounttype": 2,
 * "roleid": "785e2b66-36b0-11e7-a516-0e043877b6cb",
 * "roletype": "DomainAdmin",
 * "rolename": "Domain Admin",
 * "domainid": "adomainid",
 * "domain": "adomain",
 * "apikey": "anapikey",
 * "secretkey": "asecretkey",
 * "accountid": "anaccountid",
 * "iscallerchilddomain": false,
 * "isdefault": false
 * }],
 * "isdefault": false,
 * "groups": []* 		}]
 * }
 * }
 */
public class ListAccountsResponse {
    @SerializedName(LIST_ACCOUNTS_KEY_JSON)
    private AccountsResponse accountsResponse;

    public class AccountsResponse {
        @SerializedName(ACCOUNT_KEY_JSON)
        private List<Account> accounts;
    }

    public class Account {
        @SerializedName(USER_KEY_JSON)
        private List<User> users;

        public List<User> getUsers() {
            return users;
        }
    }

    public List<Account> getAccounts() {
        return accountsResponse.accounts;
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
