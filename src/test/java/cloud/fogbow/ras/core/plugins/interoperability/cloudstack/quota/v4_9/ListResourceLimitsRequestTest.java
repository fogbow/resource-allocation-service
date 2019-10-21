package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class ListResourceLimitsRequestTest {

    // test case: When calling the build method without specific parameters in order to get
    // all quota, it must verify if It generates the right URL.
    @Test
    public void testBuildWithoutParameters() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Quota.LIST_RESOURCE_LIMITS_COMMAND);
        String urlBaseExpected = uriBuilder.toString();

        String[] urlStructure = new String[] {
                urlBaseExpected,
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        ListResourceLimitsRequest listResourceLimitsRequest = new ListResourceLimitsRequest.Builder()
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String listResourceLimitsRequestUrl = listResourceLimitsRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, listResourceLimitsRequestUrl);
    }

    // test case: When calling the build method with specific parameters in order to get
    // a specific quota, it must verify if It generates the right URL.
    @Test
    public void testBuildWithParameters() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Quota.LIST_RESOURCE_LIMITS_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String domainId = "domainId";
        String resourceType = "resourceType";

        String domainIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Quota.DOMAIN_ID_KEY_JSON, domainId);
        String resourceTypeIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Quota.RESOURCE_TYPE_KEY_JSON, resourceType);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                domainIdStructureUrl,
                resourceTypeIdStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        ListResourceLimitsRequest listResourceLimitsRequest = new ListResourceLimitsRequest.Builder()
                .domainId(domainId)
                .resourceType(resourceType)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String listResourceLimitsRequestUrl = listResourceLimitsRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, listResourceLimitsRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new ListResourceLimitsRequest.Builder().build(null);
    }

}
