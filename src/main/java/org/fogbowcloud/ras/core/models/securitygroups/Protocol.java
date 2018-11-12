package org.fogbowcloud.ras.core.models.securitygroups;

public enum Protocol {
    TCP("tcp"), UDP("udp"), ICMP("icmp"), ANY("any");

    private String protocol;

    Protocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return this.protocol;
    }
}
