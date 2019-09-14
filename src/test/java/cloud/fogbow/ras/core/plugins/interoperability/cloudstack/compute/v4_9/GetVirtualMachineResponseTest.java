package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4.VirtualMachineTemplate;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetVirtualMachineResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    // test case: create GetVirtualMachineResponse from Cloudstack Json response but it comes empty
    @Test
    public void testGetVirtualMachineEmptyResponseFromJson() throws Exception {
        // set up
        String getVirtualMachineResponseJson = CloudstackTestUtils
                .createGetVirtualMachineEmptyResponseJson();

        // execute
        GetVirtualMachineResponse getVirtualMachineResponse =
                GetVirtualMachineResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        Assert.assertTrue(getVirtualMachineResponse.getVirtualMachines().isEmpty());
    }

    // test case: create GetVirtualMachineResponse from Cloudstack Json response but it comes with error
    @Test
    public void testGetVirtualMachineEmptyErrorResponseFromJson() throws Exception {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String getVirtualMachineResponseJson = CloudstackTestUtils
                .createGetVirtualMachineErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        GetVirtualMachineResponse.fromJson(getVirtualMachineResponseJson);
    }


}
