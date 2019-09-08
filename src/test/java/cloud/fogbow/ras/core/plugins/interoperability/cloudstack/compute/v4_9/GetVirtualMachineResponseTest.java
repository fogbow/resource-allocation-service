package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4.VirtualMachineTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GetVirtualMachineResponseTest {

    // test case: create GetVirtualMachineResponse from Cloudstack Json Response
    @Test
    public void testGetVirtualMachineResponseFromJson() throws Exception {
        // set up
        String id = "1";
        String name = "name";
        String state = "state";
        int cpuNumber = 2;
        int memory = 2;
        List<GetVirtualMachineResponse.Nic> nics = new ArrayList<>();
        nics.add(new GetVirtualMachineResponse().new Nic("id1"));
        nics.add(new GetVirtualMachineResponse().new Nic("id2"));
        String getVirtualMachineResponseJson = CloudstackTestUtils
                .createGetVirtualMachineResponseJson(id, name, state, memory, cpuNumber, nics);

        // execute
        GetVirtualMachineResponse getVirtualMachineResponse =
                GetVirtualMachineResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        GetVirtualMachineResponse.VirtualMachine firstVirtualMachine =
                getVirtualMachineResponse.getVirtualMachines().get(0);
        Assert.assertEquals(id, firstVirtualMachine.getId());
        Assert.assertEquals(name, firstVirtualMachine.getName());
        Assert.assertEquals(state, firstVirtualMachine.getState());
        Assert.assertEquals(cpuNumber, firstVirtualMachine.getCpuNumber());
        Assert.assertEquals(memory, firstVirtualMachine.getMemory());
        Assert.assertEquals(nics.size(), firstVirtualMachine.getNic().length);
    }

}
