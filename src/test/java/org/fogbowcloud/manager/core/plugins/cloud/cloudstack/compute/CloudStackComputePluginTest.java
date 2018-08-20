package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9.CloudStackComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network.CloudStackNetworkPlugin;
import org.junit.Before;
import org.junit.Test;

public class CloudStackComputePluginTest {

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
    }

    @Test
    public void testGetInstance() {

    }

    @Test
    public void testTemp() throws UnexpectedException, FogbowManagerException {
        CloudStackComputePlugin p = new CloudStackComputePlugin();

        String apiKey = "Xp2TRynMLZpFAchsTpLpcj8zW_omWCWaP6NNxmb2fV9Nv_Ga6J8QcNRbPCcZUSY2NDS83d7svGhdikV7XrVkcQ";
        String secretKey = "pnDkW2amt9w-9pjn5tB4DAIc50bCdK3m6CO99r_r5xoDTTJjpormiTfj_5QEbbkVhdE5mHbTq5t8X-fKhHcJeg";

        String tokenValue = apiKey + CloudStackTokenGenerator.TOKEN_VALUE_SEPARATOR + secretKey;
        CloudStackToken token = new CloudStackToken(tokenValue);
        String computeInstanceId = "97637962-2244-4159-b72c-120834757514";
        ComputeInstance i = p.getInstance(computeInstanceId, token);
        System.out.println(i.getLocalIpAddress());
    }

}
