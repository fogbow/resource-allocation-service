package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import org.junit.Assert;
import org.junit.Test;

public class CreateTokenRequestTest {

    public static final String EXPECTED_REQUEST_STRING = "{\"auth\":{\"identity\":{\"methods\":"
        + "[\"password\"],\"password\":{\"user\":{\"domain\":{\"name\":\"LSD\"},\"name\":"
        + "\"fogbow\",\"password\":\"c24313A4a31a\"}}},\"scope\":{\"project\":{\"domain\":"
        + "{\"name\":\"LSD\"},\"name\":\"atmosphere-workers\"}}}}";

    @Test
    public void testCompleteTokenRequestToJson() {
        CreateTokenRequest request = new CreateTokenRequest.Builder()
            .projectName(PROJECT_NAME)
            .password(PASSWORD)
            .userId(USER)
            .domain(DOMAIN)
            .build();

        Assert.assertEquals(
            EXPECTED_REQUEST_STRING, request.toJson());
    }
}
