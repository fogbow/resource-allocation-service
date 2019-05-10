package cloud.fogbow.ras.api.http.response;

public class PublicIpInstance extends OrderInstance {
    private String ip;
    private String computeName;
    private String computeId;

    public PublicIpInstance(String id) {
        super(id);
    }

    public PublicIpInstance(String id, String cloudState, String ip) {
        super(id, cloudState);
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public String getComputeName() {
        return computeName;
    }

    public void setComputeName(String computeName) {
        this.computeName = computeName;
    }

    public String getComputeId() {
        return computeId;
    }

    public void setComputeId(String computeId) {
        this.computeId = computeId;
    }
}
