package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.List;

public class ListResourceLimitsResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right ListResourceLimitsResponse.
    @Test
    public void testFromJsonSuccessfully() throws Exception {
        // set up
        String domainId = "domainId";
        String resourceType = "resourceType";
        int max = 1;
        String listResourceLimitsResponseJson = CloudstackTestUtils.createListResourceLimitsResponseJson(
                domainId, resourceType, max);

        // execute
        ListResourceLimitsResponse listResourceLimitsResponse =
                ListResourceLimitsResponse.fromJson(listResourceLimitsResponseJson);

        // verify
        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = listResourceLimitsResponse
                .getResourceLimits();
        Assert.assertEquals(1, resourceLimits.size());
        ListResourceLimitsResponse.ResourceLimit firstResourceLimit = resourceLimits.listIterator().next();
        Assert.assertEquals(domainId, firstResourceLimit.getDomainId());
        Assert.assertEquals(resourceType, firstResourceLimit.getResourceType());
        Assert.assertEquals(max, firstResourceLimit.getMax());
    }

    // test case: when creating ListResourceLimitsResponse from CloudStack Json Response
    // but it comes empty values
    @Test
    public void testFromJsonWithEmptyResult() throws Exception {
        // set up
        String listResourceLimitsEmptyResponseJson = CloudstackTestUtils
                .createListResourceLimitsEmptyResponseJson();

        // execute
        ListResourceLimitsResponse listResourceLimitsResponse =
                ListResourceLimitsResponse.fromJson(listResourceLimitsEmptyResponseJson);

        // verify
        Assert.assertTrue(listResourceLimitsResponse.getResourceLimits().isEmpty());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It throws a HttpResponseException.
    @Test
    public void testFromJsonFail() throws IOException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String listResourceLimitsErrorResponseJson = CloudstackTestUtils
                .createListResourceLimitsErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        ListResourceLimitsResponse.fromJson(listResourceLimitsErrorResponseJson);
    }

}
