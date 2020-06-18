package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class GetAllServiceOfferingsResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    // test case: create GetAllServiceOfferingsResponse from Cloudstack Json Response but it comes empty
    @Test
    public void testGetAllServiceOfferingsEmptyResponseFromJson() throws Exception {
        // set up
        String getAllServiceOfferingsResponseJson = CloudstackTestUtils
                .createGetAllServiceOfferingsEmptyResponseJson();

        // execute
        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                GetAllServiceOfferingsResponse.fromJson(getAllServiceOfferingsResponseJson);

        // verify
        Assert.assertTrue(getAllServiceOfferingsResponse.getServiceOfferings().isEmpty());
    }

    // test case: create GetAllServiceOfferingsResponse from error Cloudstack Json Response
    @Test
    public void testGetAllServiceOfferingsResponseUnexpectedJson() throws IOException, FogbowException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String getAllServiceOfferingsResponseJson = CloudstackTestUtils
                .createGetAllServiceOfferingsErrotResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        GetAllServiceOfferingsResponse.fromJson(getAllServiceOfferingsResponseJson);
    }
}