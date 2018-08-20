package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

import java.util.*;

public class CloudStackNetworkPlugin implements NetworkPlugin<CloudStackToken> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackNetworkPlugin.class);

    protected String networkOfferingId = null;
    protected String zoneId = null;

    private HttpRequestClientUtil client;

    public CloudStackNetworkPlugin() {
        // TODO read attributes from file
        this.networkOfferingId = null;
        this.zoneId = null;

        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudStackToken cloudStackToken) throws FogbowManagerException, UnexpectedException {
        SubnetUtils.SubnetInfo subnetInfo = getSubnetInfo(networkOrder.getAddress());
        if (subnetInfo == null) {
            throw new InvalidParameterException("cidr <" + networkOrder.getAddress() + "> is not valid");
        }

        String name = "fogbow_network_" + ((int) Math.random());
        String startingIp = subnetInfo.getLowAddress();
        String endingIp = subnetInfo.getHighAddress();
        String gateway = networkOrder.getGateway();

        CreateNetworkRequest request = new CreateNetworkRequest.Builder()
                .name(name)
                .displayText(name)
                .networkOfferingId(this.networkOfferingId)
                .zoneId(this.zoneId)
                .startIp(startingIp)
                .endingIp(endingIp)
                .gateway(gateway)
                .netmask(subnetInfo.getNetmask())
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        CreateNetworkResponse response = CreateNetworkResponse.fromJson(jsonResponse);
        return response.getId();
    }

    private SubnetUtils.SubnetInfo getSubnetInfo(String cidrNotation) {
        try {
            return new SubnetUtils(cidrNotation).getInfo();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public NetworkInstance getInstance(String networkInstanceId, CloudStackToken cloudStackToken) throws FogbowManagerException, UnexpectedException {
        GetNetworkRequest request = new GetNetworkRequest.Builder()
                .id(networkInstanceId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetNetworkResponse response = GetNetworkResponse.fromJson(jsonResponse);
        List<GetNetworkResponse.Network> networks = response.getNetworks();

        if (networks.size() > 0) {
            // since an id were specified, there should be no more than one network in the response
            return getNetworkInstance(networks.get(0));
        } else {
            throw new InstanceNotFoundException();
        }
    }

    @Override
    public void deleteInstance(String networkInstanceId, CloudStackToken localUserAttributes) throws FogbowManagerException, UnexpectedException {

    }

    private NetworkInstance getNetworkInstance(GetNetworkResponse.Network network) {
        String state = network.getState();
        InstanceState fogbowState = CloudStackStateMapper.map(ResourceType.NETWORK, state);

        String networkId = network.getId();
        String label = network.getName();
        String address = network.getCidr();
        String gateway = network.getGateway();
        NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;

        return new NetworkInstance(networkId, fogbowState, label, address, gateway, null, allocationMode,
                null, null, null);
    }

//    protected static final String COMMAND = "command";
//    protected static final String CREATE_NETWORK_COMMAND = "createNetwork";
//    protected static final String DELETE_NETWORK_COMMAND = "deleteNetwork";
//    protected static final String LIST_NETWORK_COMMAND   = "listNetworks";
//
//    private static final int SC_PARAM_ERROR = 431;
//    private static final int SC_INSUFFICIENT_CAPACITY_ERROR = 533;
//    private static final int SC_RESOURCE_UNAVAILABLE_ERROR = 534;
//    private static final int SC_RESOURCE_ALLOCATION_ERROR = 535;
//
//    protected static final String NETWORK_ID = "id";
//    protected static final String NETWORK_DISPLAY_NAME = "displaytext";
//    protected static final String NETWORK_NAME         = "name";
//    protected static final String NETWORK_OFFERING_ID  = "networkofferingid";
//    protected static final String NETWORK_ZONE_ID      = "zoneid";
//    protected static final String NETWORK_START_IP     = "startip";
//    protected static final String NETWORK_END_IP       = "endip";
//    protected static final String NETWORK_GATEWAY      = "gateway";
//    protected static final String NETWORK_NETMASK      = "netmask";
//    protected static final String NETWORK_START_IPV6   = "startipv6";
//    protected static final String NETWORK_END_IPV6     = "endipv6";
//    protected static final String NETWORK_GATEWAY_IPV6 = "ip6gateway";
//    protected static final String NETWORK_NETMASK_IPV6 = "ip6cidr";
//
//    private Properties properties;
//
//    private String displayText;
//    private String name;
//    private String networkOfferingiId;
//    private String zoneId;
//    private String startIp;
//    private String endIp;
//    private String gateway;
//    private String netmask;
//
//    private boolean isIPv6 = false;
//
//    public CloudStackNetworkPlugin (Properties properties) {
//        this(properties, new HttpClientWrapper());
//    }
//
//    public CloudStackNetworkPlugin(Properties properties, HttpClientWrapper httpClient) {
//        this.properties = properties;
//        this.httpClient = httpClient;
//        this.endpoint = this.properties.getProperty("network_cloudstack_api_url");
//        this.zoneId = this.properties.getProperty("network_cloudstack_zone_id");
//        this.networkOfferingiId = this.properties.getProperty("network_cloudstack_netoffering_id");
//    }
//
//    @Override
//    public String requestInstance(Token token, List<Category> categories, Map<String, String> xOCCIAtt) {
//
//        displayText = "fogbow_netdisp" + (int) (Math.random() * 100000);
//        name = "fogbow_netname" + (int) (Math.random() * 100000);
//
//        this.parseAttributes(xOCCIAtt);
//        this.validateParametersCreateNetwork();
//
//        URIBuilder uriBuilder = createURIBuilder(endpoint, CREATE_NETWORK_COMMAND);
//
//        uriBuilder.addParameter(NETWORK_NAME, name);
//        uriBuilder.addParameter(NETWORK_DISPLAY_NAME, displayText);
//        uriBuilder.addParameter(NETWORK_OFFERING_ID, networkOfferingiId);
//        uriBuilder.addParameter(NETWORK_ZONE_ID, zoneId);
//
//        if(isIPv6){
//            uriBuilder.addParameter(NETWORK_START_IPV6, startIp);
//            uriBuilder.addParameter(NETWORK_END_IPV6, endIp);
//            uriBuilder.addParameter(NETWORK_GATEWAY_IPV6, gateway);
//            uriBuilder.addParameter(NETWORK_NETMASK_IPV6, netmask);
//        }else{
//            uriBuilder.addParameter(NETWORK_START_IP, startIp);
//            uriBuilder.addParameter(NETWORK_END_IP, endIp);
//            uriBuilder.addParameter(NETWORK_GATEWAY, gateway);
//            uriBuilder.addParameter(NETWORK_NETMASK, netmask);
//        }
//
//        CloudStackHelper.sign(uriBuilder, token.getAccessId());
//        LOGGER.error("@@@@@@@@@@@@" + uriBuilder.toString());
//        HttpResponseWrapper response = httpClient.doPost(uriBuilder.toString());
//        checkStatusResponse(response.getStatusLine());
//        try {
//            JSONObject createnetworkresponse = new JSONObject(response.getContent()).optJSONObject(
//                    "createnetworkresponse");
//            JSONObject network = createnetworkresponse.getJSONObject("network");
//            return network.optString("id");
//        } catch (JSONException e) {
//            throw new OCCIException(ErrorType.BAD_REQUEST,
//                    ResponseConstants.IRREGULAR_SYNTAX);
//        }
//    }
//
//    @Override
//    public Instance getInstance(Token token, String instanceId) {
//
//        URIBuilder uriBuilder = createURIBuilder(endpoint, LIST_NETWORK_COMMAND);
//        uriBuilder.addParameter(NETWORK_ID, instanceId);
//
//        CloudStackHelper.sign(uriBuilder, token.getAccessId());
//        HttpResponseWrapper response = httpClient.doPost(uriBuilder.toString());
//        checkStatusResponse(response.getStatusLine());
//        try {
//            JSONObject listnetworkresponse = new JSONObject(response.getContent()).optJSONObject(
//                    "listnetworksresponse");
//            JSONArray network = listnetworkresponse.getJSONArray("network");
//            return createInstance(network.getJSONObject(0));
//        } catch (JSONException e) {
//            throw new OCCIException(ErrorType.BAD_REQUEST,
//                    ResponseConstants.IRREGULAR_SYNTAX);
//        }
//    }
//
//    @Override
//    public void removeInstance(Token token, String instanceId) {
//
//        URIBuilder uriBuilder = createURIBuilder(endpoint, DELETE_NETWORK_COMMAND);
//        uriBuilder.addParameter(NETWORK_ID, instanceId);
//
//        CloudStackHelper.sign(uriBuilder, token.getAccessId());
//        HttpResponseWrapper response = httpClient.doPost(uriBuilder.toString());
//        checkStatusResponse(response.getStatusLine());
//    }
//
//    private void parseAttributes(Map<String, String> xOCCIAtt){
//
//        //Mount IPS
//
//        String networkAddres = xOCCIAtt.get(OCCIConstants.NETWORK_ADDRESS);
//        Integer cidr;
//
//        if(IPAddress.isValidIPv4WithNetmask(networkAddres) ){
//
//            String[] ipSplit = networkAddres.split("/");
//            this.startIp = ipSplit[0];
//            cidr = Integer.parseInt(ipSplit[1]);
//
//            String[] endIpSplit = startIp.split("\\.");
//            StringBuilder endIpBuilder = new StringBuilder();
//            int blockCount = 0;
//            for(String ipBlock : endIpSplit){
//                blockCount++;
//                if(endIpBuilder.length()> 0){
//                    endIpBuilder.append(".");
//                }
//                if(blockCount == 4){
//                    ipBlock = "254";
//                }
//                endIpBuilder.append(ipBlock);
//
//            }
//
//            int[] ipblocks = new int[4];
//            String actualByte = "";
//
//            int index = 0;
//
//            for(int count=1; count<=32; count++){
//
//                int value = 0;
//                if(count <= cidr.intValue()){
//                    value = 1;
//                }
//
//                actualByte = actualByte+value;
//                if(actualByte.length() == 8){
//                    ipblocks[index++] = Integer.parseInt(actualByte, 2);
//                    actualByte ="";
//                }
//
//            }
//
//            StringBuilder mask = new StringBuilder();
//
//            for(int ippos=0; ippos < ipblocks.length; ippos++){
//                if(mask.length() > 0){
//                    mask.append(".");
//                }
//                mask.append(ipblocks[ippos]);
//            }
//            this.netmask = mask.toString();
//            this.endIp = endIpBuilder.toString();
//
//        }else if(IPAddress.isValidIPv6WithNetmask(networkAddres)){
//            //TODO Mount IPV6 addresses
//            isIPv6 = true;
//        }
//
//        this.gateway = xOCCIAtt.get(OCCIConstants.NETWORK_GATEWAY);
//
//    }
//
//    private Instance createInstance(JSONObject network) {
//
//        List<Resource> resources = new ArrayList<Resource>();
//        resources.add(ResourceRepository.getInstance().get(OrderConstants.NETWORK_TERM));
//
//        Map<String, String> attributes = new HashMap<String, String>();
//        attributes.put(OCCIConstants.ID, network.optString("id"));
//        attributes.put(OCCIConstants.TITLE, network.optString("name"));
//        attributes.put(OCCIConstants.NETWORK_LABEL, "");
//        attributes.put(OCCIConstants.NETWORK_VLAN,  "");
//        attributes.put(OCCIConstants.NETWORK_STATE, network.optString("state"));
//        attributes.put(OCCIConstants.NETWORK_ADDRESS,  network.optString("cidr"));
//        attributes.put(OCCIConstants.NETWORK_GATEWAY,  network.optString("gateway"));
//        attributes.put(OCCIConstants.NETWORK_ALLOCATION,
//                OCCIConstants.NetworkAllocation.DYNAMIC.getValue());
//
//        return new Instance(gateway, resources, attributes, new ArrayList<Link>(), null);
//    }
//
//    public void validateParametersCreateNetwork(){
//
//        if(displayText == null){
//            LOGGER.error("Display text must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//        if(name == null){
//            LOGGER.error("Name must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//        if(networkOfferingiId == null){
//            LOGGER.error("Network Offering ID must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//        if(zoneId == null){
//            LOGGER.error("Zone ID must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//        if(startIp == null || (!IPAddress.isValidIPv4(startIp) && !IPAddress.isValidIPv6(startIp))){
//            LOGGER.error("Network start address must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//        if(endIp == null || (!IPAddress.isValidIPv4(endIp) && !IPAddress.isValidIPv6(endIp))){
//            LOGGER.error("Network end address must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//        if(gateway == null){
//            LOGGER.error("Gateway must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//
//    }
//
//    public void validateParametersDeleteNetwork(String id){
//        if(id == null){
//            LOGGER.error("Id must be specified.");
//            throw new OCCIException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
//        }
//    }
//
//    public void validateParametersListNetwork(){
//        //TODO - Verify how to list network (filter by???)
//
//
//    }
//
//    protected static URIBuilder createURIBuilder(String endpoint, String command) {
//        try {
//            URIBuilder uriBuilder = new URIBuilder(endpoint);
//            uriBuilder.addParameter(COMMAND, command);
//            return uriBuilder;
//        } catch (Exception e) {
//            throw new OCCIException(ErrorType.BAD_REQUEST,
//                    ResponseConstants.IRREGULAR_SYNTAX);
//        }
//    }
//
//    protected void checkStatusResponse(StatusLine statusLine) {
//        if (statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
//            throw new OCCIException(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
//        } else if (statusLine.getStatusCode() == SC_PARAM_ERROR) {
//            throw new OCCIException(ErrorType.NOT_FOUND,
//                    ResponseConstants.NOT_FOUND + " | Details: " + statusLine.getReasonPhrase());
//        } else if (statusLine.getStatusCode() == SC_INSUFFICIENT_CAPACITY_ERROR ||
//                statusLine.getStatusCode() == SC_RESOURCE_UNAVAILABLE_ERROR) {
//            throw new OCCIException(ErrorType.NO_VALID_HOST_FOUND, ResponseConstants.NO_VALID_HOST_FOUND);
//        } else if (statusLine.getStatusCode() == SC_RESOURCE_ALLOCATION_ERROR) {
//            throw new OCCIException(ErrorType.QUOTA_EXCEEDED,
//                    ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES);
//        } else if (statusLine.getStatusCode() > 204) {
//            throw new OCCIException(ErrorType.BAD_REQUEST, statusLine.getReasonPhrase());
//        }
//    }

}
