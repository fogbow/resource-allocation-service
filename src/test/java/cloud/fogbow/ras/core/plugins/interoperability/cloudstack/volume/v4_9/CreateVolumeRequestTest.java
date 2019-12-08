package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class CreateVolumeRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Volume.CREATE_VOLUME_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String size = "1";
        String zoneId = "2";
        String diskOfferingId = "3";
        String name = "name";

        String sizeStructureUrl = CloudstackTestUtils.buildParameterStructureUrl(
                CloudStackConstants.Volume.SIZE_KEY_JSON, size);
        String zoneIdStructureUrl = CloudstackTestUtils.buildParameterStructureUrl(
                CloudStackConstants.Volume.ZONE_ID_KEY_JSON, zoneId);
        String diskOfferingIdStructureUrl = CloudstackTestUtils.buildParameterStructureUrl(
                CloudStackConstants.Volume.DISK_OFFERING_ID_KEY_JSON, diskOfferingId);
        String nameStructureUrl = CloudstackTestUtils.buildParameterStructureUrl(
                CloudStackConstants.Volume.NAME_KEY_JSON, name);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                zoneIdStructureUrl,
                nameStructureUrl,
                diskOfferingIdStructureUrl,
                sizeStructureUrl,
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest.Builder()
                .name(name)
                .diskOfferingId(diskOfferingId)
                .zoneId(zoneId)
                .size(size)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String createVolumeRequestUrl = createVolumeRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, createVolumeRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new CreateVolumeRequest.Builder().build(null);
    }

}
