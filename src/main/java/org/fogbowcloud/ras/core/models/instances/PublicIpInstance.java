package org.fogbowcloud.ras.core.models.instances;

public class PublicIpInstance extends Instance {

    private String ip;

    public PublicIpInstance(String id) {
        super(id);
    }

    public PublicIpInstance(String id, InstanceState state, String ip) {
        super(id, state);
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

}
