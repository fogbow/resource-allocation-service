package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CloudStackComputeQuotaPlugin implements ComputeQuotaPlugin {

    private HttpRequestClientUtil client = new HttpRequestClientUtil();

    @Override
    public ComputeQuota getUserQuota(Token token) throws FogbowRasException, UnexpectedException {
        ListResourceLimitsRequest request = new ListResourceLimitsRequest();
        CloudStackUrlUtil.sign(request.getUriBuilder(), token.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), token);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(jsonResponse);
        response.getResourceLimits();
        return null;
    }

    public ResourcesInfo getResourcesInfo(CloudStackToken token) {
        URIBuilder uriBuilder = createURIBuilder(endpoint, LIST_RESOURCE_LIMITS_COMMAND);
        CloudStackHelper.sign(uriBuilder, token.getAccessId());
        HttpResponseWrapper response = httpClient.doGet(uriBuilder.toString());
        checkStatusResponse(response);

        int instancesQuota = 0;
        int cpuQuota = 0;
        int memQuota = 0;

        try {
            JSONArray limitsJson = new JSONObject(response.getContent()).optJSONObject(
                    "listresourcelimitsresponse").optJSONArray("resourcelimit");
            for (int i = 0; limitsJson != null && i < limitsJson.length(); i++) {
                JSONObject limit = limitsJson.optJSONObject(i);
                int max = limit.optInt("max") < 0 ? Integer.MAX_VALUE : limit.optInt("max");
                int capacityType = limit.optInt("resourcetype");
                switch (capacityType) {
                    case LIMIT_TYPE_INSTANCES:
                        instancesQuota = max;
                        break;
                    case LIMIT_TYPE_CPU:
                        cpuQuota = max;
                        break;
                    case LIMIT_TYPE_MEMORY:
                        memQuota = max;
                        break;
                    default:
                        break;
                }
            }
        } catch (JSONException e) {
            throw new OCCIException(ErrorType.BAD_REQUEST,
                    ResponseConstants.IRREGULAR_SYNTAX);
        }

        List<Instance> instances = getInstances(token);
        Integer cpuInUse = 0;
        Integer memInUse = 0;
        Integer instancesInUse = instances.size();
        for (Instance instance : instances) {
            cpuInUse += Integer.valueOf(
                    instance.getAttributes().get("occi.compute.cores"));
            memInUse += (int) (Double.valueOf(
                    instance.getAttributes().get("occi.compute.memory")) * 1024);
        }

        ResourcesInfo resInfo = new ResourcesInfo(
                String.valueOf(cpuQuota - cpuInUse), cpuInUse.toString(),
                String.valueOf(memQuota - memInUse), memInUse.toString(),
                String.valueOf(instancesQuota - instancesInUse), instancesInUse.toString());
        return resInfo;
    }


    class ResourcesInfo {

        private String id;
        private String cpuIdle;
        private String cpuInUse;
        private String memIdle;
        private String memInUse;
        private String instancesIdle;
        private String instancesInUse;

        private String cpuInUseByUser;
        private String memInUseByUser;
        private String instancesInUseByUser;

        private static final String ZERO = "0";

        public ResourcesInfo() {
            setId(ZERO);
            setCpuIdle(ZERO);
            setCpuInUse(ZERO);
            setMemIdle(ZERO);
            setMemInUse(ZERO);
            setInstancesIdle(ZERO);
            setInstancesInUse(ZERO);

            setCpuInUseByUser(ZERO);
            setMemInUseByUser(ZERO);
            setInstancesInUseByUser(ZERO);
        }

        public ResourcesInfo(String id, String cpuIdle, String cpuInUse,
                             String memIdle, String memInUse, String instancesIdle,
                             String instancesInUse, String cpuInUseByUser,
                             String memInUseByUser, String instancesInUseByUser) {
            this(id, cpuIdle, cpuInUse, memIdle, memInUse, instancesIdle, instancesInUse);
            this.cpuInUseByUser = cpuInUseByUser;
            this.memInUseByUser = memInUseByUser;
            this.instancesInUseByUser = instancesInUseByUser;

        }

        public ResourcesInfo(String id, String cpuIdle, String cpuInUse,
                             String memIdle, String memInUse,
                             String instancesIdle, String instancesInUse) {
            this();
            setId(id);
            setCpuIdle(cpuIdle);
            setCpuInUse(cpuInUse);
            setMemIdle(memIdle);
            setMemInUse(memInUse);
            setInstancesIdle(instancesIdle);
            setInstancesInUse(instancesInUse);
        }

        public ResourcesInfo(String cpuIdle, String cpuInUse,
                             String memIdle, String memInUse,
                             String instancesIdle, String instancesInUse) {
            this(null, cpuIdle, cpuInUse, memIdle, memInUse,
                    instancesIdle, instancesInUse);
        }

        public ResourcesInfo(String cpuInUseByUser, String memInUseByUser,
                             String instancesInUseByUser) {
            this();
            this.cpuInUseByUser = cpuInUseByUser;
            this.memInUseByUser = memInUseByUser;
            this.instancesInUseByUser = instancesInUseByUser;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCpuIdle() {
            return cpuIdle;
        }

        public void setCpuIdle(String cpuIdle) {
            if (cpuIdle == null) {
                throw new IllegalArgumentException(
                        "ResourceInfo cpu-idle is invalid.");
            }
            this.cpuIdle = cpuIdle;
        }

        public String getCpuInUse() {
            return cpuInUse;
        }

        public void setCpuInUse(String cpuInUse) {
            if (cpuInUse == null) {
                throw new IllegalArgumentException(
                        "ResourceInfo cpu-inuse is invalid.");
            }
            this.cpuInUse = cpuInUse;
        }

        public String getMemIdle() {
            return memIdle;
        }

        public void setMemIdle(String memIdle) {
            if (memIdle == null) {
                throw new IllegalArgumentException(
                        "ResourceInfo mem-idle is invalid.");
            }
            this.memIdle = memIdle;
        }

        public String getMemInUse() {
            return memInUse;
        }

        public void setMemInUse(String memInUse) {
            if (memInUse == null) {
                throw new IllegalArgumentException(
                        "ResourceInfo mem-inuse is invalid.");
            }
            this.memInUse = memInUse;
        }

        public void setInstancesIdle(String instancesIdle) {
            this.instancesIdle = instancesIdle;
        }

        public String getInstancesIdle() {
            return instancesIdle;
        }

        public void setInstancesInUse(String instancesInUse) {
            this.instancesInUse = instancesInUse;
        }

        public String getInstancesInUse() {
            return instancesInUse;
        }

        public String getCpuInUseByUser() {
            return cpuInUseByUser;
        }

        public void setCpuInUseByUser(String cpuInUseByUser) {
            this.cpuInUseByUser = cpuInUseByUser;
        }

        public String getMemInUseByUser() {
            return memInUseByUser;
        }

        public void setMemInUseByUser(String memInUseByUser) {
            this.memInUseByUser = memInUseByUser;
        }

        public String getInstancesInUseByUser() {
            return instancesInUseByUser;
        }

        public void setInstancesInUseByUser(String insntacesInUseByUser) {
            this.instancesInUseByUser = insntacesInUseByUser;
        }

        @Override
        public String toString() {
            return "ResourcesInfo [id=" + id + ", cpuIdle=" + cpuIdle + ", cpuInUse=" + cpuInUse + ", cpuInUseByUser=" + cpuInUseByUser
                    + ", memIdle=" + memIdle + ", memInUse=" + memInUse + ", memInUseByUser=" + memInUseByUser + ", instancesIdle="
                    + instancesIdle + ", instancesInUse=" + instancesInUse + ", instancesInUseByUser=" + instancesInUseByUser
                    + ", ZERO=" + ZERO + "]";
        }

    }


}
