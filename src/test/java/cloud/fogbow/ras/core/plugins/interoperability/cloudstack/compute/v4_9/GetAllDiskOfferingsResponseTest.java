package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GetAllDiskOfferingsResponseTest {

    // test case: create GetAllDiskOfferingsResponse from Cloudstack Json Response
    @Test
    public void testGetAllDiskOfferingsResponse() throws IOException, FogbowException {
        // set up
        String id = "id";
        int disk = 2;
        String tags = "tags";
        boolean customized = false;
        String json = CloudstackTestUtils.createGetAllDiskOfferingsResponseJson(
                id, disk, customized, tags);

        // exercise
        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse =
                GetAllDiskOfferingsResponse.fromJson(json);
        GetAllDiskOfferingsResponse.DiskOffering firstDiskOffering =
                getAllDiskOfferingsResponse.getDiskOfferings().get(0);

        //verify
        Assert.assertEquals(id, firstDiskOffering.getId());
        Assert.assertEquals(disk, firstDiskOffering.getDiskSize());
        Assert.assertEquals(customized, firstDiskOffering.isCustomized());
        Assert.assertEquals(tags, firstDiskOffering.getTags());
    }

    // test case: create GetAllDiskOfferingsResponse from Cloudstack Json Response but it comes empty
    @Test
    public void testGetAllDiskOfferingsEmptyResponse() throws IOException, FogbowException {
        // set up
        String json = CloudstackTestUtils.createGetAllDiskOfferingsEmptyResponseJson();

        // exercise
        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse =
                GetAllDiskOfferingsResponse.fromJson(json);

        //verify
        Assert.assertTrue(getAllDiskOfferingsResponse.getDiskOfferings().isEmpty());
    }

    // test case: create GetAllDiskOfferingsResponse from unexpected Cloudstack Json Response
    @Test(expected = FogbowException.class)
    public void testGetAllDiskOfferingsResponseUnexpectedJson() throws FogbowException {
        // set up
        String unexpectedJsonResponse = "{}";

        // execute and verify
        GetAllDiskOfferingsResponse.fromJson(unexpectedJsonResponse);
    }

}
