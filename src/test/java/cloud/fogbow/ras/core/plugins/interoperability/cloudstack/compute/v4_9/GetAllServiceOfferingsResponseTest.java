package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import org.junit.Assert;
import org.junit.Test;

public class GetAllServiceOfferingsResponseTest {

    // test case: create GetAllServiceOfferingsResponse from Cloudstack Json Response
    @Test
    public void testGetAllServiceOfferingsResponseFromJson() throws Exception {
        // set up
        String id = "1";
        String name = "name";
        int cpuNumber = 2;
        int memory = 2;
        String tags = "tags";
        String getAllServiceOfferingsResponseJson = CloudstackTestUtils
                .createGetAllServiceOfferingsResponseJson(id, name, cpuNumber, memory, tags);

        // execute
        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                GetAllServiceOfferingsResponse.fromJson(getAllServiceOfferingsResponseJson);

        // verify
        GetAllServiceOfferingsResponse.ServiceOffering firstServiceOffering =
                getAllServiceOfferingsResponse.getServiceOfferings().get(0);
        Assert.assertEquals(id, firstServiceOffering.getId());
        Assert.assertEquals(name, firstServiceOffering.getName());
        Assert.assertEquals(cpuNumber, firstServiceOffering.getCpuNumber());
        Assert.assertEquals(memory, firstServiceOffering.getMemory());
        Assert.assertEquals(tags, firstServiceOffering.getTags());
    }

    // test case: create GetAllServiceOfferingsResponse from unexpected Cloudstack Json Response
    @Test(expected = FogbowException.class)
    public void testGetAllServiceOfferingsResponseUnexpectedJson() throws FogbowException {
        // set up
        String unexpectedJsonResponse = "{}";

        // execute and verify
        GetAllServiceOfferingsResponse.fromJson(unexpectedJsonResponse);
    }
}