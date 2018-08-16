package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import org.fogbowcloud.manager.util.GsonHolder;

import java.util.List;

/*
 * {
 *     "listnetworksresponse": {
 *         "count": 8,
 *         "network": [{
 *             "id": "dcd137e5-1ed8-4917-b3ef-e56e0e5f2290",
 *             "name": "F",
 *             "displaytext": "F",
 *             "broadcastdomaintype": "Vlan",
 *             "traffictype": "Guest",
 *             "gateway": "10.1.1.1",
 *             "netmask": "255.255.255.0",
 *             "cidr": "10.1.1.0/24",
 *             "zoneid": "0d89768b-bdf5-455e-b4fd-91881fa07375",
 *             "zonename": "CDC-REC01",
 *             "networkofferingid": "edf05b4c-5dea-4c4b-a1f9-57f5b3d787f3",
 *             "networkofferingname": "Default Network",
 *             "networkofferingdisplaytext": "Default Network",
 *             "networkofferingconservemode": true,
 *             "networkofferingavailability": "Optional",
 *             "issystem": false,
 *             "state": "Allocated",
 *             "related": "dcd137e5-1ed8-4917-b3ef-e56e0e5f2290",
 *             "dns1": "200.133.37.13",
 *             "dns2": "200.139.34.203",
 *             "type": "Isolated",
 *             "acltype": "Account",
 *             "account": "marcosancj@lsd.ufcg.edu.br",
 *             "domainid": "0dbcf598-4d56-4b8a-8a6f-343fd0956392",
 *             "domain": "FOGBOW",
 *             "service": [{
 *                 "name": "PortForwarding"
 *             }, {
 *                 "name": "StaticNat"
 *             }, {
 *                 "name": "Firewall",
 *                 "capability": [{
 *                     "name": "TrafficStatistics",
 *                     "value": "per public ip",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedEgressProtocols",
 *                     "value": "tcp,udp,icmp, all",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedProtocols",
 *                     "value": "tcp,udp,icmp",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "MultipleIps",
 *                     "value": "true",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedTrafficDirection",
 *                     "value": "ingress, egress",
 *                     "canchooseservicecapability": false
 *                 }]
 *             }, {
 *                 "name": "Dhcp",
 *                 "capability": [{
 *                     "name": "DhcpAccrossMultipleSubnets",
 *                     "value": "true",
 *                     "canchooseservicecapability": false
 *                 }]
 *             }, {
 *                 "name": "Dns",
 *                 "capability": [{
 *                     "name": "AllowDnsSuffixModification",
 *                     "value": "true",
 *                     "canchooseservicecapability": false
 *                 }]
 *             }, {
 *                 "name": "Lb",
 *                 "capability": [{
 *                     "name": "SupportedLBIsolation",
 *                     "value": "dedicated",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedStickinessMethods",
 *                     "value": "[{\"methodname\":\"LbCookie\",\"paramlist\":[{\"paramname\":\"cookie-name\",\"required\":false,\"isflag\":false,\"description\":\" \"},{\"paramname\":\"mode\",\"required\":false,\"isflag\":false,\"description\":\" \"},{\"paramname\":\"nocache\",\"required\":false,\"isflag\":true,\"description\":\" \"},{\"paramname\":\"indirect\",\"required\":false,\"isflag\":true,\"description\":\" \"},{\"paramname\":\"postonly\",\"required\":false,\"isflag\":true,\"description\":\" \"},{\"paramname\":\"domain\",\"required\":false,\"isflag\":false,\"description\":\" \"}],\"description\":\"This is loadbalancer cookie based stickiness method.\"},{\"methodname\":\"AppCookie\",\"paramlist\":[{\"paramname\":\"cookie-name\",\"required\":false,\"isflag\":false,\"description\":\" \"},{\"paramname\":\"length\",\"required\":false,\"isflag\":false,\"description\":\" \"},{\"paramname\":\"holdtime\",\"required\":false,\"isflag\":false,\"description\":\" \"},{\"paramname\":\"request-learn\",\"required\":false,\"isflag\":true,\"description\":\" \"},{\"paramname\":\"prefix\",\"required\":false,\"isflag\":true,\"description\":\" \"},{\"paramname\":\"mode\",\"required\":false,\"isflag\":false,\"description\":\" \"}],\"description\":\"This is App session based sticky method. Define session stickiness on an existing application cookie. It can be used only for a specific http traffic\"},{\"methodname\":\"SourceBased\",\"paramlist\":[{\"paramname\":\"tablesize\",\"required\":false,\"isflag\":false,\"description\":\" \"},{\"paramname\":\"expire\",\"required\":false,\"isflag\":false,\"description\":\" \"}],\"description\":\"This is source based Stickiness method, it can be used for any type of protocol.\"}]",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "AutoScaleCounters",
 *                     "value": "[{\"methodname\":\"cpu\",\"paramlist\":[]},{\"methodname\":\"memory\",\"paramlist\":[]}]",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedProtocols",
 *                     "value": "tcp, udp, tcp-proxy",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "LbSchemes",
 *                     "value": "Public",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedLbAlgorithms",
 *                     "value": "roundrobin,leastconn,source",
 *                     "canchooseservicecapability": false
 *                 }]
 *             }, {
 *                 "name": "UserData"
 *             }, {
 *                 "name": "Vpn",
 *                 "capability": [{
 *                     "name": "VpnTypes",
 *                     "value": "removeaccessvpn",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "SupportedVpnTypes",
 *                     "value": "pptp,l2tp,ipsec",
 *                     "canchooseservicecapability": false
 *                 }]
 *             }, {
 *                 "name": "SourceNat",
 *                 "capability": [{
 *                     "name": "SupportedSourceNatTypes",
 *                     "value": "peraccount",
 *                     "canchooseservicecapability": false
 *                 }, {
 *                     "name": "RedundantRouter",
 *                     "value": "true",
 *                     "canchooseservicecapability": false
 *                 }]
 *             }],
 *             "networkdomain": "cs53cloud.internal",
 *             "physicalnetworkid": "fb892012-f5be-443e-a9ca-6a9fe13dceaa",
 *             "restartrequired": false,
 *             "specifyipranges": false,
 *             "canusefordeploy": true,
 *             "ispersistent": false,
 *             "tags": [],
 *             "strechedl2subnet": false
 *         }]
 *     }
 * }
 */
public class GetNetworkResponse {


    public static GetNetworkResponse fromJson(String jsonResponse) {
        return GsonHolder.getInstance().fromJson(jsonResponse, GetNetworkResponse.class);
    }

    public List<Network> getNetworks() {
        throw new UnsupportedOperationException();
    }

    public class Network {

        public String getId() {
            throw new UnsupportedOperationException();
        }

        public String getState() {
            throw new UnsupportedOperationException();
        }

        public String getLabel() {
            throw new UnsupportedOperationException();
        }

        public String getAddress() {
            throw new UnsupportedOperationException();
        }

        public String getGateway() {
            throw new UnsupportedOperationException();
        }
    }
}
