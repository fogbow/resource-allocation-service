package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import org.fogbowcloud.ras.util.GsonHolder;
import org.junit.Assert;
import org.junit.Test;

public class CreateTokenRequestTest {

    public static final String DOMAIN = "FAKE_DOMAIN";
    public static final String USER_NAME = "FAKE_USERNAME";
    public static final String PASSWORD = "FAKE_PASSWORD";
    public static final String PROJECT_NAME = "FAKE_PROJECT_NAME";

    public static final String EXPECTED_REQUEST_STRING = String.format("{"
        + "    \"auth\":{"
        + "        \"identity\":{"
        + "            \"methods\":["
        + "                \"password\""
        + "            ],"
        + "            \"password\":{"
        + "                \"user\":{"
        + "                    \"domain\":{"
        + "                        \"name\":\"%s\""
        + "                    },"
        + "                    \"name\":\"%s\","
        + "                    \"password\":\"%s\""
        + "                }"
        + "            }"
        + "        },"
        + "        \"scope\":{"
        + "            \"project\":{"
        + "                \"domain\":{"
        + "                    \"name\":\"%s\""
        + "                },"
        + "                \"name\":\"%s\""
        + "            }"
        + "        }"
        + "    }"
        + "}", DOMAIN, USER_NAME, PASSWORD, DOMAIN, PROJECT_NAME);

    @Test
    public void testCompleteTokenRequestToJson() {
        CreateTokenRequest request = new CreateTokenRequest.Builder()
            .projectName(PROJECT_NAME)
            .password(PASSWORD)
            .userName(USER_NAME)
            .domain(DOMAIN)
            .build();

        CreateTokenRequest expectedRequest = GsonHolder.getInstance().fromJson(EXPECTED_REQUEST_STRING, CreateTokenRequest.class);
        Assert.assertEquals(expectedRequest, request);
    }
}
