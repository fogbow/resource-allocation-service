package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class GetAllDiskOfferingsResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    // test case: create GetAllDiskOfferingsResponse from error Cloudstack Json Response
    @Test
    public void testGetAllDiskOfferingsResponseUnexpectedJson() throws IOException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String json = CloudstackTestUtils.createGetAllDiskOfferingsErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // exercise
        GetAllDiskOfferingsResponse.fromJson(json);
    }

}
