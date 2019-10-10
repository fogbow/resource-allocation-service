package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GetAllDiskOfferingsRequestTest {

    // test case: create GetAllDiskOfferingsRequestUrl successfully
    @Test
    public void testCreateGetAllDiskOfferingsRequestUrl() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND);
        String urlExpected = uriBuilder.toString();

        // exercise
        GetAllDiskOfferingsRequest getAllDiskOfferingsRequest = new GetAllDiskOfferingsRequest
                .Builder().build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String getAllDiskOfferingsRequestUrl = getAllDiskOfferingsRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpected, getAllDiskOfferingsRequestUrl);
    }

    // test case: trying create GetAllDiskOfferingsRequestUrl but it occur an error
    @Test(expected = InvalidParameterException.class)
    public void testCreateGetAllDiskOfferingsRequestWithError() throws InvalidParameterException {
        // exercise and verify
        new GetAllDiskOfferingsRequest.Builder().build(null);
    }
}
