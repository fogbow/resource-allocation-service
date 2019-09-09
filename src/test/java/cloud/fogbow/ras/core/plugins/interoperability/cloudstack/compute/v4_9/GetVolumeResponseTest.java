package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import org.junit.Assert;
import org.junit.Test;

public class GetVolumeResponseTest {

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

}
