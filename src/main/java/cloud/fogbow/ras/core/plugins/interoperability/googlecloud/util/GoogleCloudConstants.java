package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;

public class GoogleCloudConstants {
    public static final String PATH_PROJECT = "/projects";
    public static final String LINE_SEPARATOR = "/";
    public static final String GLOBAL_IP_ENDPOINT = "/global/addresses";

    public static final class SecurityRule {
        public static final EtherType ETHER_TYPE = EtherType.IPv4;
    }

}
