package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class EnableStaticNatRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.PublicIp.ENABLE_STATIC_NAT_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String virtualMachineId = "virtualMachineId";
        String ipAddressId = "ipAdressId";

        String virtualMachineIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.VM_ID_KEY_JSON, virtualMachineId);
        String ipAddressIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.IP_ADDRESS_ID_KEY_JSON, ipAddressId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                virtualMachineIdStructureUrl,
                ipAddressIdStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        EnableStaticNatRequest enableStaticNatRequest = new EnableStaticNatRequest.Builder()
                .virtualMachineId(virtualMachineId)
                .ipAddressId(ipAddressId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String associateIpAddressRequestUrl = enableStaticNatRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, associateIpAddressRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new EnableStaticNatRequest.Builder().build(null);
    }

}
