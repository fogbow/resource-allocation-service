package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.CreateVolumeResponse;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class CreateVolumeResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right CreateVolumeResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException, FogbowException {
        // set up
        String idExpected = "id";
        String createVolumeResponseJson = CloudstackTestUtils.createCreateVolumeResponseJson(idExpected);

        // execute
        CreateVolumeResponse createVolumeResponse = CreateVolumeResponse.fromJson(createVolumeResponseJson);

        // verify
        Assert.assertEquals(idExpected, createVolumeResponse.getId());
    }

    // it must verify if It throws a FogbowException.
    @Test
    public void testFromJsonFail() throws IOException, FogbowException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils.createCreateVolumeErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        CreateVolumeResponse.fromJson(volumesErrorResponseJson);
    }

}
