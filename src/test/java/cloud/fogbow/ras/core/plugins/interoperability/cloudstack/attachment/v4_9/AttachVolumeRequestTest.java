package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class AttachVolumeRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InternalServerErrorException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Attachment.ATTACH_VOLUME_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String volumeId = "volumeId";
        String virtualMachineId = "virtualMachineId";

        String volumeIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Attachment.ID_KEY_JSON, volumeId);
        String virtualMachineIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Attachment.VIRTUAL_MACHINE_ID_KEY_JSON, virtualMachineId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                volumeIdStructureUrl,
                virtualMachineIdStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest.Builder()
                .id(volumeId)
                .virtualMachineId(virtualMachineId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String attachVolumeRequestUrl = attachVolumeRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, attachVolumeRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testBuildFail() throws InternalServerErrorException {
        // exercise and verify
        new AttachVolumeRequest.Builder().build(null);
    }

}
