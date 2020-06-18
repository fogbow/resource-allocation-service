package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class GetVolumeResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: create GetVolumeResponse from Cloudstack Json Response
    @Test
    public void testGetVolumeResponseFromJson() throws Exception {
        // set up
        String id = "1";
        String name = "name";
        String state = "state";
        int size = 2;
        String getVirtualMachineResponseJson = CloudstackTestUtils
                .createGetVolumesResponseJson(id, name, size, state);

        // execute
        GetVolumeResponse getVolumeResponse =
                GetVolumeResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        GetVolumeResponse.Volume volume = getVolumeResponse.getVolumes().get(0);
        Assert.assertEquals(id, volume.getId());
        Assert.assertEquals(name, volume.getName());
        Assert.assertEquals(state, volume.getState());
        Assert.assertEquals(size, volume.getSize());
    }

    // test case: create GetVolumeResponse from Cloudstack Json Response with empty values
    @Test
    public void testGetVolumeResponseFromJsonEmptyValue() throws Exception {
        // set up
        String getVirtualMachineResponseJson = CloudstackTestUtils.createEmptyGetVolumesResponseJson();

        // execute
        GetVolumeResponse getVolumeResponse =
                GetVolumeResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        Assert.assertTrue(getVolumeResponse.getVolumes().isEmpty());
    }

    // test case: create GetVolumeResponse from error Cloudstack Json Response
    @Test
    public void testGetVolumeErrorResponseJson() throws IOException, FogbowException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createGetVolumesErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        GetVolumeResponse.fromJson(volumesErrorResponseJson);
    }
}
