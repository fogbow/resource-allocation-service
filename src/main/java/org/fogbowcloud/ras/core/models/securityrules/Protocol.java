package org.fogbowcloud.ras.core.models.securityrules;

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
