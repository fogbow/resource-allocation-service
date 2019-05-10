package cloud.fogbow.ras.api.http.response;

public class VolumeInstance extends OrderInstance {
    private String name;
    private int volumeSize;

    public VolumeInstance(String id, String cloudState, String name, int volumeSize) {
        super(id, cloudState);
        this.name = name;
        this.volumeSize = volumeSize;
    }

    public VolumeInstance(String id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVolumeSize() {
        return this.volumeSize;
    }

    public void setVolumeSize(int volumeSize) {
        this.volumeSize = volumeSize;
    }
}
