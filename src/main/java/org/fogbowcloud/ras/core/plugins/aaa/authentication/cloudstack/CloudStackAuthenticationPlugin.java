package org.fogbowcloud.ras.core.plugins.aaa.authentication.cloudstack;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack.v3.OpenStackAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;

public class CloudStackAuthenticationPlugin extends RASAuthenticationPlugin {

        private static final Logger LOGGER = Logger.getLogger(CloudStackAuthenticationPlugin.class);

        public CloudStackAuthenticationPlugin() {
        }

        @Override
        protected String getTokenMessage(FederationUserToken federationUserToken) {
            CloudStackToken cloudStackToken = (CloudStackToken) federationUserToken;
            String[] parameters = new String[] {
                    cloudStackToken.getTokenProvider(),
                    cloudStackToken.getTokenValue(),
                    cloudStackToken.getUserId(),
                    cloudStackToken.getUserName()};
            return StringUtils.join(parameters, CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR);
        }

        @Override
        protected String getSignature(FederationUserToken federationUserToken) {
            CloudStackToken cloudStackToken = (CloudStackToken) federationUserToken;
            return cloudStackToken.getSignature();
        }
}
