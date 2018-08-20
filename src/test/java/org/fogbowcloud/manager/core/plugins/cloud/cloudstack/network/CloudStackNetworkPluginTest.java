package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator;
import org.junit.Before;
import org.junit.Test;

public class CloudStackNetworkPluginTest {

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
    }

    @Test
    public void testTemp() throws UnexpectedException, FogbowManagerException {
        CloudStackNetworkPlugin p = new CloudStackNetworkPlugin();

        String apiKey = "Xp2TRynMLZpFAchsTpLpcj8zW_omWCWaP6NNxmb2fV9Nv_Ga6J8QcNRbPCcZUSY2NDS83d7svGhdikV7XrVkcQ";
        String secretKey = "pnDkW2amt9w-9pjn5tB4DAIc50bCdK3m6CO99r_r5xoDTTJjpormiTfj_5QEbbkVhdE5mHbTq5t8X-fKhHcJeg";

        String tokenValue = apiKey + CloudStackTokenGenerator.TOKEN_VALUE_SEPARATOR + secretKey;
        CloudStackToken token = new CloudStackToken(tokenValue);
        String networkId = "6fc82e5a-1d96-4d10-b9bf-96693c89dda8";
        NetworkInstance i = p.getInstance(networkId, token);

    }

}
