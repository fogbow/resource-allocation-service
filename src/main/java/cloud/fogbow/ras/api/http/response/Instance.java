package cloud.fogbow.ras.api.http.response;

public class Instance {
    private String id;
    private InstanceState state;
    private String cloudState;
    private boolean isReady;
    private boolean hasFailed;
    private String provider;
    private String cloudName;

    public Instance(String id) {
        this.id = id;
        this.isReady = false;
        this.hasFailed = false;
    }

    public Instance(String id, String cloudState) {
        this(id);
        this.cloudState = cloudState;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InstanceState getState() {
        return this.state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public String getCloudState() {
        return cloudState;
    }

    public void setCloudState(String cloudState) {
        cloudState = cloudState;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady() {
        isReady = true;
        hasFailed = false;
    }

    public boolean hasFailed() {
        return hasFailed;
    }

    public void setHasFailed() {
        this.hasFailed = true;
        this.isReady = false;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getCloudName() {
        return cloudName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Instance other = (Instance) obj;
        if (this.id == null) {
            if (other.getId() != null) return false;
        } else if (!this.id.equals(other.getId())) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "id='" + id + '\'' +
                ", state=" + state +
                ", provider='" + provider + '\'' +
                ", cloudName='" + cloudName + '\'' +
                '}';
    }
}
